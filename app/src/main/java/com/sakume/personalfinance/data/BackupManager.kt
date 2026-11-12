package com.sakume.personalfinance.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.sakume.personalfinance.ui.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup otomatis.
 *
 *  - Satu file per hari: sakume_backup_2026-11-13.json (ditimpa dengan data terbaru hari itu).
 *  - Tidak menulis ulang kalau data & pengaturan tidak berubah sejak backup terakhir.
 *  - Disimpan di penyimpanan internal app + (opsional) disalin ke folder pilihan pengguna,
 *    supaya selamat saat app di-uninstall / data dihapus.
 *  - Tidak pernah menimpa backup bagus dengan database kosong.
 */
object BackupManager {
    private const val DIR = "backups"
    private const val PREFIX_DAILY = "sakume_backup_"
    private const val PREFIX_PRE_IMPORT = "sakume_sebelum_impor_"
    private const val KEEP_PRE_IMPORT = 3

    private val mutex = Mutex()

    enum class Kind { DAILY, PRE_IMPORT }

    enum class Status { SAVED, SKIPPED, FAILED }

    data class Outcome(val status: Status, val message: String, val folderError: String? = null)

    data class BackupEntry(
        val name: String,
        val createdAtMillis: Long,
        val sizeBytes: Long,
        val kind: Kind,
        val accountCount: Int,
        val transactionCount: Int,
        /** false bila file tidak bisa dibaca / bukan backup SakuMe yang valid. */
        val valid: Boolean,
        /** true bila entri ini berasal dari folder pilihan pengguna (bukan penyimpanan internal). */
        val inFolder: Boolean,
        val file: File?,
        val uri: Uri?
    )

    // ------------------------------------------------------------------ pengaturan

    fun currentSettings(context: Context) = BackupSettings(
        trailingZeros = AppPrefs.trailingZeros(context),
        notificationsEnabled = AppPrefs.notificationsEnabled(context),
        maxCatchupDays = AppPrefs.maxCatchupDays(context),
        billReminderEnabled = AppPrefs.billReminderEnabled(context),
        billReminderHour = AppPrefs.billReminderHour(context),
        billReminderEarlyDays = AppPrefs.billReminderEarlyDays(context),
        autoBackupEnabled = AppPrefs.autoBackupEnabled(context),
        autoBackupKeep = AppPrefs.autoBackupKeep(context)
    )

    /** Hanya menimpa pengaturan yang ADA di file backup; sisanya dibiarkan. */
    fun applySettings(context: Context, s: BackupSettings?) {
        if (s == null) return
        s.trailingZeros?.let { AppPrefs.setTrailingZeros(context, it) }
        s.notificationsEnabled?.let { AppPrefs.setNotificationsEnabled(context, it) }
        s.maxCatchupDays?.let { AppPrefs.setMaxCatchupDays(context, it) }
        s.billReminderEnabled?.let { AppPrefs.setBillReminderEnabled(context, it) }
        s.billReminderHour?.let { AppPrefs.setBillReminderHour(context, it) }
        s.billReminderEarlyDays?.let { AppPrefs.setBillReminderEarlyDays(context, it) }
        s.autoBackupEnabled?.let {
            AppPrefs.setAutoBackupEnabled(context, it)
            if (it) BackupWorker.enqueuePeriodic(context) else BackupWorker.cancel(context)
        }
        s.autoBackupKeep?.let { AppPrefs.setAutoBackupKeep(context, it) }
        // Data baru saja diganti -> backup berikutnya harus benar-benar menulis file.
        AppPrefs.setAutoBackupState(context, "", AppPrefs.autoBackupLastMillis(context))
    }

    // ------------------------------------------------------------------ menjalankan backup

    /**
     * @param force true = abaikan saklar "otomatis" dan pemeriksaan "tidak ada perubahan" (tombol Backup sekarang).
     */
    suspend fun runBackup(context: Context, force: Boolean = false, source: String = "auto"): Outcome =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    if (!force && !AppPrefs.autoBackupEnabled(context)) {
                        return@withLock Outcome(Status.SKIPPED, "Backup otomatis nonaktif")
                    }
                    val repo = FinanceRepository(AppDatabase.get(context))
                    val data = repo.snapshot()
                    if (data.accounts.isEmpty() && data.transactions.isEmpty() && data.bills.isEmpty() &&
                        data.debts.isEmpty() && data.investments.isEmpty()
                    ) {
                        // Jangan sampai database kosong (mis. habis hapus data) menimpa backup yang bagus.
                        return@withLock Outcome(Status.SKIPPED, "Belum ada data untuk dibackup")
                    }
                    val settings = currentSettings(context)
                    val hash = sha256(BackupCodec.encode(stable(data).copy(settings = settings)))
                    if (!force && hash == AppPrefs.autoBackupLastHash(context)) {
                        return@withLock Outcome(Status.SKIPPED, "Tidak ada perubahan sejak backup terakhir")
                    }

                    val now = System.currentTimeMillis()
                    val json = BackupCodec.encode(
                        data.copy(
                            meta = BackupMeta(
                                createdAtMillis = now,
                                appVersion = appVersion(context),
                                source = source
                            ),
                            settings = settings
                        )
                    )
                    // Pastikan file yang akan ditulis benar-benar bisa dibaca balik sebelum menyimpannya.
                    val back = BackupCodec.decode(json)
                    check(back.copy(meta = null, settings = null) == data) { "Verifikasi backup gagal" }

                    val name = PREFIX_DAILY + dayStamp(now) + ".json"
                    writeAtomic(File(dir(context), name), json)
                    pruneInternal(context)

                    var folderError: String? = null
                    AppPrefs.autoBackupFolder(context)?.let { folder ->
                        folderError = copyToFolder(context, folder, name, json)
                        if (folderError == null) pruneFolder(context, folder)
                    }

                    // Hash baru disimpan hanya bila semua tujuan berhasil, supaya kegagalan folder dicoba lagi.
                    if (folderError == null) AppPrefs.setAutoBackupState(context, hash, now)
                    else AppPrefs.setAutoBackupState(context, "", now)

                    Outcome(Status.SAVED, name, folderError)
                } catch (e: Exception) {
                    Outcome(Status.FAILED, e.message ?: "Backup gagal")
                }
            }
        }

    /** JSON lengkap (semua tabel + pengaturan + info versi) untuk ekspor / bagikan manual. */
    suspend fun buildFullJson(context: Context, source: String = "export"): String = withContext(Dispatchers.IO) {
        val data = FinanceRepository(AppDatabase.get(context)).snapshot()
        BackupCodec.encode(
            data.copy(
                meta = BackupMeta(createdAtMillis = System.currentTimeMillis(), appVersion = appVersion(context), source = source),
                settings = currentSettings(context)
            )
        )
    }

    /** Dipanggil tepat sebelum impor/restore: simpan kondisi saat ini supaya impor bisa dibatalkan. */
    suspend fun savePreImport(context: Context, currentJson: String): String = withContext(Dispatchers.IO) {
        val name = PREFIX_PRE_IMPORT + SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date()) + ".json"
        // Bungkus dengan meta supaya muncul rapi di daftar.
        val wrapped = try {
            val data = BackupCodec.decode(currentJson)
            BackupCodec.encode(
                data.copy(
                    meta = BackupMeta(
                        createdAtMillis = System.currentTimeMillis(),
                        appVersion = appVersion(context),
                        source = "pre-import"
                    ),
                    settings = currentSettings(context)
                )
            )
        } catch (_: Exception) {
            currentJson
        }
        writeAtomic(File(dir(context), name), wrapped)
        pruneInternal(context)
        name
    }

    // ------------------------------------------------------------------ daftar & baca

    suspend fun list(context: Context): List<BackupEntry> = withContext(Dispatchers.IO) {
        val result = mutableListOf<BackupEntry>()

        dir(context).listFiles { f -> f.isFile && f.name.endsWith(".json") && kindOf(f.name) != null }
            ?.forEach { f ->
                result += buildEntry(f.name, f.length(), f.lastModified(), inFolder = false, file = f, uri = null) {
                    f.readText()
                }
            }

        val folder = AppPrefs.autoBackupFolder(context)
        if (folder != null) {
            try {
                val tree = DocumentFile.fromTreeUri(context, Uri.parse(folder))
                tree?.listFiles()?.forEach { doc ->
                    val n = doc.name ?: return@forEach
                    if (!doc.isFile || !n.endsWith(".json") || kindOf(n) == null) return@forEach
                    // Kalau file yang sama sudah ada di penyimpanan internal, tidak perlu ditampilkan dua kali.
                    if (result.any { it.name == n && it.sizeBytes == doc.length() }) return@forEach
                    result += buildEntry(n, doc.length(), doc.lastModified(), inFolder = true, file = null, uri = doc.uri) {
                        context.contentResolver.openInputStream(doc.uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("Tidak bisa membuka file")
                    }
                }
            } catch (_: Exception) {
                // Folder tidak bisa diakses lagi; daftar internal tetap ditampilkan.
            }
        }
        result.sortedByDescending { it.createdAtMillis }
    }

    suspend fun readText(context: Context, entry: BackupEntry): String = withContext(Dispatchers.IO) {
        entry.file?.readText()
            ?: entry.uri?.let { u -> context.contentResolver.openInputStream(u)?.bufferedReader()?.use { it.readText() } }
            ?: error("File backup tidak ditemukan")
    }

    suspend fun delete(context: Context, entry: BackupEntry) = withContext(Dispatchers.IO) {
        entry.file?.delete()
        entry.uri?.let { u -> DocumentFile.fromSingleUri(context, u)?.delete() }
        Unit
    }

    /** Null = tidak ada folder dipilih. Selain itu: true bila folder masih bisa ditulis. */
    fun folderWritable(context: Context): Boolean? {
        val folder = AppPrefs.autoBackupFolder(context) ?: return null
        return try {
            DocumentFile.fromTreeUri(context, Uri.parse(folder))?.canWrite() == true
        } catch (_: Exception) {
            false
        }
    }

    fun folderLabel(context: Context): String? {
        val folder = AppPrefs.autoBackupFolder(context) ?: return null
        return try {
            DocumentFile.fromTreeUri(context, Uri.parse(folder))?.name
        } catch (_: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------ internal

    private fun dir(context: Context): File =
        File(context.filesDir, DIR).also { if (!it.exists()) it.mkdirs() }

    private fun kindOf(name: String): Kind? = when {
        name.startsWith(PREFIX_DAILY) -> Kind.DAILY
        name.startsWith(PREFIX_PRE_IMPORT) -> Kind.PRE_IMPORT
        else -> null
    }

    private fun buildEntry(
        name: String,
        size: Long,
        lastModified: Long,
        inFolder: Boolean,
        file: File?,
        uri: Uri?,
        read: () -> String
    ): BackupEntry {
        val kind = kindOf(name) ?: Kind.DAILY
        return try {
            val data = BackupCodec.decode(read())
            BackupEntry(
                name, data.meta?.createdAtMillis?.takeIf { it > 0 } ?: lastModified, size, kind,
                data.accounts.size, data.transactions.size, true, inFolder, file, uri
            )
        } catch (_: Exception) {
            BackupEntry(name, lastModified, size, kind, 0, 0, false, inFolder, file, uri)
        }
    }

    private fun writeAtomic(target: File, text: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) {
            target.writeText(text)
            tmp.delete()
        }
    }

    private fun pruneInternal(context: Context) {
        val files = dir(context).listFiles()?.filter { it.isFile } ?: return
        files.filter { it.name.endsWith(".tmp") }.forEach { it.delete() }
        val keep = AppPrefs.autoBackupKeep(context)
        files.filter { kindOf(it.name) == Kind.DAILY }
            .sortedByDescending { it.name }
            .drop(keep).forEach { it.delete() }
        files.filter { kindOf(it.name) == Kind.PRE_IMPORT }
            .sortedByDescending { it.name }
            .drop(KEEP_PRE_IMPORT).forEach { it.delete() }
    }

    private fun copyToFolder(context: Context, folderUri: String, name: String, json: String): String? {
        return try {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
                ?: return "Folder backup tidak ditemukan"
            if (!tree.canWrite()) return "Folder backup tidak bisa ditulis. Pilih ulang folder."
            val existing = tree.findFile(name)
            val target = existing ?: tree.createFile("application/json", name.removeSuffix(".json"))
                ?: return "Gagal membuat file di folder backup"
            context.contentResolver.openOutputStream(target.uri, "wt")?.bufferedWriter()?.use { it.write(json) }
                ?: return "Gagal menulis ke folder backup"
            null
        } catch (e: Exception) {
            "Gagal menyalin ke folder: ${e.message ?: "tidak diketahui"}"
        }
    }

    private fun pruneFolder(context: Context, folderUri: String) {
        try {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(folderUri)) ?: return
            val keep = AppPrefs.autoBackupKeep(context)
            tree.listFiles()
                .filter { it.isFile && it.name?.let { n -> kindOf(n) == Kind.DAILY } == true }
                .sortedByDescending { it.name }
                .drop(keep)
                .forEach { it.delete() }
        } catch (_: Exception) {
        }
    }

    /** Urutan baris dari SQLite bisa berubah walau isi sama (INSERT OR REPLACE); urutkan agar hash stabil. */
    private fun stable(d: BackupData) = d.copy(
        accounts = d.accounts.sortedBy { it.id },
        transactions = d.transactions.sortedBy { it.id },
        bills = d.bills.sortedBy { it.id },
        investments = d.investments.sortedBy { it.id },
        contributions = d.contributions.sortedBy { it.id },
        debts = d.debts.sortedBy { it.id },
        schedules = d.schedules.sortedBy { it.id },
        salaries = d.salaries.sortedBy { it.id },
        holdings = d.holdings.sortedBy { it.id }
    )

    private fun dayStamp(ms: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))

    private fun appVersion(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

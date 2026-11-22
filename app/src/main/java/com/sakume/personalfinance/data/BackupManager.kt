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

object BackupManager {
    private const val DIR = "backups"
    private const val PREFIX_DAILY = "sakume_backup_"
    private const val PREFIX_PRE_IMPORT = "sakume_sebelum_impor_"
    private const val KEEP_PRE_IMPORT = 3
    private const val AGE_PRUNE_MIN_COUNT = 4
    private const val HEAD_CHARS = 512
    private const val MAGIC_PW1 = "SAKUMEENCPW1"

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
        val valid: Boolean,
        val inFolder: Boolean,
        val file: File?,
        val uri: Uri?,
        val passwordProtected: Boolean = false,
        val foreignKey: Boolean = false
    )

    data class UpgradeResult(val upgraded: Int, val failed: Int)

    fun currentSettings(context: Context) = BackupSettings(
        notificationsEnabled = AppPrefs.notificationsEnabled(context),
        maxCatchupDays = AppPrefs.maxCatchupDays(context),
        billReminderEnabled = AppPrefs.billReminderEnabled(context),
        billReminderHour = AppPrefs.billReminderHour(context),
        billReminderEarlyDays = AppPrefs.billReminderEarlyDays(context),
        autoBackupEnabled = AppPrefs.autoBackupEnabled(context),
        autoBackupKeep = AppPrefs.autoBackupKeep(context)
    )

    fun applySettings(context: Context, s: BackupSettings?) {
        if (s == null) return
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
        AppPrefs.setAutoBackupState(context, "", AppPrefs.autoBackupLastMillis(context))
    }

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
                        return@withLock Outcome(Status.SKIPPED, "Belum ada data untuk dibackup")
                    }
                    val settings = currentSettings(context)
                    val hash = sha256(BackupCodec.encode(stable(data).copy(settings = settings)))
                    if (!force && hash == AppPrefs.autoBackupLastHash(context)) {
                        return@withLock Outcome(Status.SKIPPED, "Tidak ada perubahan sejak backup terakhir")
                    }

                    val hadPassword = SecureStore.hasBackupPassword(context)
                    val now = System.currentTimeMillis()
                    val payload = data.copy(
                        meta = BackupMeta(
                            createdAtMillis = now,
                            appVersion = appVersion(context),
                            source = source
                        ),
                        settings = settings
                    )
                    val encrypted = BackupCodec.encodeForBackup(context, payload)
                    val back = BackupCodec.decodeMaybeEncrypted(context, encrypted)
                    check(back.copy(meta = null, settings = null) == data) { "Verifikasi backup gagal" }

                    val name = pickTodayName(context, now)
                    val isNewBackup = !File(dir(context), name).exists()
                    writeAtomic(File(dir(context), name), encrypted)
                    pruneInternal(context)
                    if (isNewBackup) pruneInternalByAge(context, now)

                    var folderError: String? = null
                    AppPrefs.autoBackupFolder(context)?.let { folder ->
                        folderError = copyToFolder(context, folder, name, encrypted)
                        if (folderError == null) {
                            pruneFolder(context, folder)
                            if (isNewBackup) pruneFolderByAge(context, folder, now)
                        }
                    }

                    val passwordUnchanged = SecureStore.hasBackupPassword(context) == hadPassword
                    if (folderError == null && passwordUnchanged) AppPrefs.setAutoBackupState(context, hash, now)
                    else AppPrefs.setAutoBackupState(context, "", now)

                    Outcome(Status.SAVED, name, folderError)
                } catch (e: Exception) {
                    Outcome(Status.FAILED, e.message ?: "Backup gagal")
                }
            }
        }

    suspend fun buildFullJson(context: Context, source: String = "export"): String = withContext(Dispatchers.IO) {
        val data = FinanceRepository(AppDatabase.get(context)).snapshot()
        val payload = data.copy(
            meta = BackupMeta(
                createdAtMillis = System.currentTimeMillis(),
                appVersion = appVersion(context),
                source = source
            ),
            settings = currentSettings(context)
        )
        BackupCodec.encodeForBackup(context, payload)
    }

    fun hasPassword(context: Context): Boolean = SecureStore.hasBackupPassword(context)

    fun setPassword(context: Context, password: CharArray): Boolean {
        val saved = SecureStore.setBackupPassword(context, password)
        if (saved) resetDedupeHash(context)
        return saved
    }

    fun clearPassword(context: Context) {
        SecureStore.clearBackupPassword(context)
        resetDedupeHash(context)
    }

    suspend fun upgradeLegacyBackups(context: Context): UpgradeResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!SecureStore.hasBackupPassword(context)) return@withLock UpgradeResult(0, 0)
            var upgraded = 0
            var failed = 0

            dir(context).listFiles { f -> f.isFile && f.name.endsWith(".json") && kindOf(f.name) != null }
                ?.forEach { f ->
                    try {
                        val head = f.bufferedReader().use { it.readHeadChars() }
                        if (!SecureCrypto.isDeviceEncrypted(head)) return@forEach
                        writeAtomic(f, reencrypt(context, f.readText()))
                        upgraded++
                    } catch (_: Exception) {
                        failed++
                    }
                }

            val folder = AppPrefs.autoBackupFolder(context)
            if (folder != null) {
                try {
                    val tree = DocumentFile.fromTreeUri(context, Uri.parse(folder))
                    tree?.listFiles()?.forEach { doc ->
                        val n = doc.name ?: return@forEach
                        if (!doc.isFile || !n.endsWith(".json") || kindOf(n) == null) return@forEach
                        try {
                            val head = withReader(context, doc.uri) { it.readHeadChars() }
                            if (!SecureCrypto.isDeviceEncrypted(head)) return@forEach
                            val old = withReader(context, doc.uri) { it.readText() }
                            val encoded = reencrypt(context, old)
                            try {
                                writeUri(context, doc.uri, encoded)
                                val written = withReader(context, doc.uri) { it.readText() }
                                check(written == encoded) { "Verifikasi tulis gagal" }
                            } catch (e: Exception) {
                                try {
                                    writeUri(context, doc.uri, old)
                                } catch (_: Exception) {
                                }
                                throw e
                            }
                            upgraded++
                        } catch (_: Exception) {
                            failed++
                        }
                    }
                } catch (_: Exception) {
                }
            }
            UpgradeResult(upgraded, failed)
        }
    }

    suspend fun savePreImport(context: Context, data: BackupData): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (data.isEmptyOfUserData()) return@withLock null
            val now = System.currentTimeMillis()
            val name = PREFIX_PRE_IMPORT + SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date(now)) + ".json"
            val payload = data.copy(
                meta = BackupMeta(
                    createdAtMillis = now,
                    appVersion = appVersion(context),
                    source = "pre-import"
                ),
                settings = currentSettings(context)
            )
            val encoded = BackupCodec.encodeForBackup(context, payload)
            val back = BackupCodec.decodeMaybeEncrypted(context, encoded)
            check(back.copy(meta = null, settings = null) == data.copy(meta = null, settings = null)) {
                "Verifikasi backup pengaman gagal"
            }
            val target = File(dir(context), name)
            writeAtomic(target, encoded)
            check(target.exists() && target.length() > 0L) { "Backup pengaman gagal ditulis" }
            pruneInternal(context)
            name
        }
    }

    suspend fun list(context: Context): List<BackupEntry> = withContext(Dispatchers.IO) {
        val result = mutableListOf<BackupEntry>()

        dir(context).listFiles { f -> f.isFile && f.name.endsWith(".json") && kindOf(f.name) != null }
            ?.forEach { f ->
                result += buildEntry(
                    context, f.name, f.length(), f.lastModified(), inFolder = false, file = f, uri = null,
                    readHead = { f.bufferedReader().use { it.readHeadChars() } },
                    readAll = { f.readText() }
                )
            }

        val folder = AppPrefs.autoBackupFolder(context)
        if (folder != null) {
            try {
                val tree = DocumentFile.fromTreeUri(context, Uri.parse(folder))
                tree?.listFiles()?.forEach { doc ->
                    val n = doc.name ?: return@forEach
                    if (!doc.isFile || !n.endsWith(".json") || kindOf(n) == null) return@forEach
                    if (result.any { it.name == n && it.sizeBytes == doc.length() }) return@forEach
                    result += buildEntry(
                        context, n, doc.length(), doc.lastModified(), inFolder = true, file = null, uri = doc.uri,
                        readHead = { withReader(context, doc.uri) { it.readHeadChars() } },
                        readAll = { withReader(context, doc.uri) { it.readText() } }
                    )
                }
            } catch (_: Exception) {
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

    private fun dir(context: Context): File =
        File(context.filesDir, DIR).also { if (!it.exists()) it.mkdirs() }

    private fun kindOf(name: String): Kind? = when {
        name.startsWith(PREFIX_DAILY) -> Kind.DAILY
        name.startsWith(PREFIX_PRE_IMPORT) -> Kind.PRE_IMPORT
        else -> null
    }

    private fun buildEntry(
        context: Context,
        name: String,
        size: Long,
        lastModified: Long,
        inFolder: Boolean,
        file: File?,
        uri: Uri?,
        readHead: () -> String,
        readAll: () -> String
    ): BackupEntry {
        val kind = kindOf(name) ?: Kind.DAILY
        fun entry(
            created: Long,
            accounts: Int,
            transactions: Int,
            valid: Boolean,
            passwordProtected: Boolean = false,
            foreignKey: Boolean = false
        ) = BackupEntry(
            name, created, size, kind, accounts, transactions, valid, inFolder, file, uri,
            passwordProtected, foreignKey
        )

        val head = try {
            readHead().trimStart()
        } catch (_: Exception) {
            return entry(lastModified, 0, 0, false)
        }

        if (SecureCrypto.isPasswordEncrypted(head)) {
            val header = SecureCrypto.readPasswordHeader(head)
            return when {
                header != null -> entry(
                    header.createdAtMillis.takeIf { it > 0 } ?: lastModified,
                    header.accounts, header.transactions, true, passwordProtected = true
                )
                head.startsWith(MAGIC_PW1) -> entry(lastModified, 0, 0, true, passwordProtected = true)
                else -> entry(lastModified, 0, 0, false, passwordProtected = true)
            }
        }

        val text = try {
            readAll()
        } catch (_: Exception) {
            return entry(lastModified, 0, 0, false)
        }
        return try {
            val data = BackupCodec.decodeMaybeEncrypted(context, text)
            entry(
                data.meta?.createdAtMillis?.takeIf { it > 0 } ?: lastModified,
                data.accounts.size, data.transactions.size, true
            )
        } catch (_: Exception) {
            entry(lastModified, 0, 0, false, foreignKey = SecureCrypto.isDeviceEncrypted(head))
        }
    }

    private fun java.io.Reader.readHeadChars(max: Int = HEAD_CHARS): String {
        val buf = CharArray(max)
        var total = 0
        while (total < max) {
            val n = read(buf, total, max - total)
            if (n <= 0) break
            total += n
        }
        return String(buf, 0, total).trimStart()
    }

    private fun <T> withReader(context: Context, uri: Uri, block: (java.io.Reader) -> T): T {
        val stream = context.contentResolver.openInputStream(uri) ?: error("Tidak bisa membuka file")
        return stream.bufferedReader().use(block)
    }

    private fun writeUri(context: Context, uri: Uri, text: String) {
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("Gagal menulis ke folder backup")
        out.bufferedWriter().use { it.write(text) }
    }

    private fun reencrypt(context: Context, text: String): String {
        val data = BackupCodec.decodeMaybeEncrypted(context, text)
        val encoded = BackupCodec.encodeForBackup(context, data)
        check(SecureCrypto.isPasswordEncrypted(encoded)) { "Password backup belum tersimpan" }
        check(BackupCodec.decodeMaybeEncrypted(context, encoded) == data) { "Verifikasi backup gagal" }
        return encoded
    }

    private fun resetDedupeHash(context: Context) {
        AppPrefs.setAutoBackupState(context, "", AppPrefs.autoBackupLastMillis(context))
    }

    private fun BackupData.isEmptyOfUserData(): Boolean =
        accounts.isEmpty() && transactions.isEmpty() && bills.isEmpty() && investments.isEmpty() &&
            contributions.isEmpty() && debts.isEmpty() && schedules.isEmpty() && salaries.isEmpty() &&
            holdings.isEmpty()

    private fun pickTodayName(context: Context, nowMillis: Long): String {
        val todayPrefix = PREFIX_DAILY + dayStamp(nowMillis)
        val base = "$todayPrefix.json"
        val own = dir(context).listFiles { f ->
            f.isFile && (f.name == base || (f.name.startsWith("${todayPrefix}_") && f.name.endsWith(".json")))
        }?.map { it.name }?.sorted()?.lastOrNull()
        if (own != null) return own
        val folder = AppPrefs.autoBackupFolder(context)
        if (folder != null && folderHasFile(context, folder, base)) {
            return "${todayPrefix}_" + SimpleDateFormat("HHmmss", Locale.US).format(Date(nowMillis)) + ".json"
        }
        return base
    }

    private fun folderHasFile(context: Context, folderUri: String, name: String): Boolean = try {
        DocumentFile.fromTreeUri(context, Uri.parse(folderUri))?.findFile(name) != null
    } catch (_: Exception) {
        false
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
        files.filter { kindOf(it.name) == Kind.DAILY && it.name.endsWith(".json") }
            .sortedByDescending { it.name }
            .drop(keep).forEach { it.delete() }
        files.filter { kindOf(it.name) == Kind.PRE_IMPORT && it.name.endsWith(".json") }
            .sortedByDescending { it.name }
            .drop(KEEP_PRE_IMPORT).forEach { it.delete() }
    }

    private fun ageInDays(name: String, nowMillis: Long): Long? {
        val stamp = name.removePrefix(PREFIX_DAILY).take(10)
        val dayStart = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(stamp)?.time
        } catch (_: Exception) {
            null
        } ?: return null
        val today = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        return Math.round((today - dayStart) / 86_400_000.0)
    }

    private fun namesToPruneByAge(names: List<String>, nowMillis: Long, maxDays: Long): Set<String> {
        if (names.size <= AGE_PRUNE_MIN_COUNT) return emptySet()
        var remaining = names.size
        val doomed = mutableSetOf<String>()
        for (n in names.sorted()) {
            if (remaining <= AGE_PRUNE_MIN_COUNT) break
            val age = ageInDays(n, nowMillis) ?: continue
            if (age > maxDays) {
                doomed += n
                remaining--
            }
        }
        return doomed
    }

    private fun pruneInternalByAge(context: Context, nowMillis: Long) {
        val files = dir(context).listFiles()?.filter { it.isFile && kindOf(it.name) == Kind.DAILY } ?: return
        val doomed = namesToPruneByAge(files.map { it.name }, nowMillis, AppPrefs.autoBackupKeep(context).toLong())
        files.filter { it.name in doomed }.forEach { it.delete() }
    }

    private fun pruneFolderByAge(context: Context, folderUri: String, nowMillis: Long) {
        try {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(folderUri)) ?: return
            val docs = tree.listFiles()
                .filter { it.isFile && it.name?.let { n -> kindOf(n) == Kind.DAILY } == true }
            val doomed = namesToPruneByAge(docs.mapNotNull { it.name }, nowMillis, AppPrefs.autoBackupKeep(context).toLong())
            docs.filter { it.name in doomed }.forEach { it.delete() }
        } catch (_: Exception) {
        }
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

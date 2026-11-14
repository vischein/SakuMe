package com.sakume.personalfinance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.BackupManager
import com.sakume.personalfinance.data.BackupManager.BackupEntry
import com.sakume.personalfinance.data.BackupWorker
import com.sakume.personalfinance.ui.AppPrefs
import com.sakume.personalfinance.ui.components.RestoreBackupDialog
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Paper
import com.sakume.personalfinance.ui.theme.Red
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    refreshKey: Int,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBackupNow: () -> Unit,
    onPickFolder: () -> Unit,
    onClearFolder: () -> Unit,
    onRestore: (BackupEntry) -> Unit,
    onDeleteEntry: (BackupEntry) -> Unit,
    onPrefsChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    var autoEnabled by remember(refreshKey) { mutableStateOf(AppPrefs.autoBackupEnabled(context)) }
    var keep by remember(refreshKey) { mutableStateOf(AppPrefs.autoBackupKeep(context)) }
    val lastMillis = remember(refreshKey) { AppPrefs.autoBackupLastMillis(context) }
    val folderLabel = remember(refreshKey) { BackupManager.folderLabel(context) }
    val folderWritable = remember(refreshKey) { BackupManager.folderWritable(context) }

    var entries by remember { mutableStateOf<List<BackupEntry>?>(null) }
    LaunchedEffect(refreshKey) { entries = BackupManager.list(context) }

    var pending by remember { mutableStateOf<BackupEntry?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Ink)
                }
                Text("Backup & Restore", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
        }

        item {
            Card {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Backup otomatis", fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp)
                        Text(
                            "Tersimpan sendiri setiap kamu menutup app (hanya jika ada perubahan) dan tiap 12 jam. " +
                                "Berisi semua data dan pengaturan.",
                            color = Muted, fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = autoEnabled,
                        onCheckedChange = { on ->
                            autoEnabled = on
                            AppPrefs.setAutoBackupEnabled(context, on)
                            if (on) BackupWorker.enqueuePeriodic(context) else BackupWorker.cancel(context)
                            onPrefsChanged()
                        },
                        colors = switchColors()
                    )
                }
                Text(
                    if (lastMillis > 0) "Terakhir disimpan: ${dateTime(lastMillis)}" else "Belum ada backup otomatis.",
                    color = Muted, fontSize = 12.sp
                )
                Text("Simpan riwayat", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AppPrefs.BACKUP_KEEP_OPTIONS.size) { i ->
                        val n = AppPrefs.BACKUP_KEEP_OPTIONS[i]
                        Chip("$n hari", keep == n) {
                            keep = n
                            AppPrefs.setAutoBackupKeep(context, n)
                        }
                    }
                }
                Button(
                    onClick = onBackupNow,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)
                ) {
                    Icon(Icons.Filled.Backup, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Backup sekarang")
                }
            }
        }

        item {
            Card {
                Text("Folder cadangan (disarankan)", fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp)
                Text(
                    "Backup di penyimpanan app ikut hilang kalau SakuMe di-uninstall atau datanya dihapus. " +
                        "Pilih sebuah folder (mis. Download/SakuMe, atau folder yang disinkronkan ke cloud) " +
                        "agar salinannya aman di luar app. Setelah install ulang, pilih folder yang sama " +
                        "dan daftar backup akan muncul lagi.",
                    color = Muted, fontSize = 12.sp
                )
                when {
                    folderLabel == null && folderWritable == null ->
                        Text("Belum ada folder dipilih.", color = Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    folderWritable == false ->
                        Text(
                            "Folder \"${folderLabel ?: "?"}\" tidak bisa diakses. Pilih ulang folder.",
                            color = Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                    else ->
                        Text("Folder: ${folderLabel ?: "dipilih"}", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onPickFolder,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Folder, null, modifier = Modifier.size(18.dp), tint = Ink)
                        Spacer(Modifier.width(6.dp))
                        Text(if (folderLabel == null && folderWritable == null) "Pilih folder" else "Ganti folder", color = Ink)
                    }
                    if (folderWritable != null) {
                        TextButton(onClick = onClearFolder) { Text("Lepas folder", color = Muted) }
                    }
                }
            }
        }

        item {
            Column {
                Text("Daftar backup", fontWeight = FontWeight.Bold, color = Ink, fontSize = 18.sp)
                Text("Ketuk salah satu untuk memulihkan data ke kondisi tanggal itu.", color = Muted, fontSize = 12.sp)
            }
        }

        val list = entries
        when {
            list == null -> item { Text("Memuat…", color = Muted, fontSize = 13.sp) }
            list.isEmpty() -> item {
                Card { Text("Belum ada backup. Ketuk \"Backup sekarang\" atau tutup app, backup akan dibuat otomatis.", color = Muted, fontSize = 13.sp) }
            }
            else -> items(list, key = { (if (it.inFolder) "f:" else "i:") + it.name }) { e ->
                BackupRow(e) { pending = e }
            }
        }

        item {
            Card {
                Text("Ekspor / impor manual", fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp)
                Text(
                    "Bagikan file backup ke tempat lain, atau pulihkan dari file JSON (termasuk backup lama). " +
                        "File tidak terenkripsi — jangan bagikan ke sembarang orang.",
                    color = Muted, fontSize = 12.sp
                )
                Button(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)
                ) {
                    Icon(Icons.Filled.FileUpload, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ekspor / Bagikan")
                }
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.FileDownload, null, modifier = Modifier.size(20.dp), tint = Ink)
                    Spacer(Modifier.width(8.dp))
                    Text("Pilih file & impor", color = Ink)
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    pending?.let { e ->
        RestoreBackupDialog(
            title = if (e.valid) "Pulihkan backup ${dateLong(e.createdAtMillis)}?" else "File backup rusak",
            detail = if (e.valid)
                "Data sekarang akan DIGANTI dengan isi backup ini (${e.accountCount} rekening, ${e.transactionCount} transaksi). " +
                    "Kondisi sekarang disimpan dulu otomatis sebagai \"Sebelum impor\", jadi masih bisa dibatalkan."
            else "File ini tidak bisa dibaca sebagai backup SakuMe. Kamu bisa menghapusnya.",
            canRestore = e.valid,
            onRestore = { pending = null; onRestore(e) },
            onDelete = { pending = null; onDeleteEntry(e) },
            onDismiss = { pending = null }
        )
    }
}

@Composable
private fun BackupRow(e: BackupEntry, onClick: () -> Unit) {
    val tag = when {
        !e.valid -> "Rusak"
        e.kind == BackupManager.Kind.PRE_IMPORT -> "Sebelum impor"
        else -> null
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardBg,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(dateLong(e.createdAtMillis), fontWeight = FontWeight.SemiBold, color = if (e.valid) Ink else Muted, fontSize = 14.sp)
                Text(
                    buildString {
                        append(timeOnly(e.createdAtMillis))
                        if (e.valid) append(" · ${e.accountCount} rekening · ${e.transactionCount} transaksi")
                        append(" · ${sizeLabel(e.sizeBytes)}")
                        if (e.inFolder) append(" · di folder")
                    },
                    color = Muted, fontSize = 12.sp
                )
            }
            if (tag != null) {
                Text(
                    tag,
                    color = if (e.valid) Ink else Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0F0F3))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) Color.White else Ink,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Ink else Color(0xFFF0F0F3))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Ink,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = Muted
)

private val idLocale = Locale("id", "ID")
private fun dateLong(ms: Long): String = SimpleDateFormat("EEEE, dd MMM yyyy", idLocale).format(Date(ms))
private fun timeOnly(ms: Long): String = SimpleDateFormat("HH.mm", idLocale).format(Date(ms))
private fun dateTime(ms: Long): String = SimpleDateFormat("EEE, dd MMM yyyy · HH.mm", idLocale).format(Date(ms))
private fun sizeLabel(bytes: Long): String = if (bytes < 1024) "$bytes B" else "${bytes / 1024} KB"

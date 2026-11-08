package com.sakume.personalfinance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Paper

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onRecalculate: () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Backup & Restore",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        Text(
            "Ekspor data ke file JSON untuk disimpan, atau impor file backup untuk memulihkan data.",
            color = Muted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ekspor backup", fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp)
                Text(
                    "Simpan seluruh data (rekening, transaksi, tagihan, investasi, piutang/utang, jadwal) ke file JSON yang bisa dibagikan.",
                    color = Muted,
                    fontSize = 13.sp
                )
                Button(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)
                ) {
                    Icon(Icons.Filled.FileUpload, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ekspor / Bagikan")
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Impor backup", fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp)
                Text(
                    "Pulihkan data dari file backup. Data saat ini akan diganti seluruhnya. Pastikan file valid.",
                    color = Muted,
                    fontSize = 13.sp
                )
                Button(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)
                ) {
                    Icon(Icons.Filled.FileDownload, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pilih file & impor")
                }
            }
        }

        Spacer(Modifier.height(14.dp))
    }
}

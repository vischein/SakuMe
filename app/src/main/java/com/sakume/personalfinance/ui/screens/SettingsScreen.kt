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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.ScheduleWorker
import com.sakume.personalfinance.ui.AppPrefs
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Paper
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    onAddSalary: () -> Unit
) {
    val context = LocalContext.current
    var trailingZeros by remember { mutableStateOf(AppPrefs.trailingZeros(context)) }
    var notifEnabled by remember { mutableStateOf(AppPrefs.notificationsEnabled(context)) }
    var lockEnabled by remember { mutableStateOf(AppPrefs.appLockEnabled(context)) }
    var catchupDays by remember { mutableStateOf(AppPrefs.maxCatchupDays(context)) }
    var showPinSetup by remember { mutableStateOf(false) }
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Ink)
            }
            Text(
                "Pengaturan",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                SettingsSection(icon = Icons.Filled.Tune, title = "Input nominal") {
                    Text(
                        "Jumlah nol otomatis di belakang saat mengisi field Nominal. " +
                            "Ketik 5 → ${exampleAmount(5, trailingZeros)}. " +
                            "Pilih Off untuk nominal exact (mis. 5.696).",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AppPrefs.OPTIONS.size) { idx ->
                            val n = AppPrefs.OPTIONS[idx]
                            val selected = trailingZeros == n
                            Chip(
                                label = AppPrefs.zerosLabel(n),
                                selected = selected
                            ) {
                                trailingZeros = n
                                AppPrefs.setTrailingZeros(context, n)
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(icon = Icons.Filled.Notifications, title = "Notifikasi") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Notifikasi jadwal", fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
                            Text(
                                "Beritahu saat gaji/investasi otomatis berhasil diproses di background.",
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = notifEnabled,
                            onCheckedChange = { on ->
                                notifEnabled = on
                                AppPrefs.setNotificationsEnabled(context, on)
                                if (on) ScheduleWorker.enqueue(context)
                                else ScheduleWorker.cancel(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Ink,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Muted
                            )
                        )
                    }
                }
            }

            item {
                SettingsSection(icon = Icons.Filled.Lock, title = "Keamanan") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Kunci aplikasi (PIN)", fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
                            Text(
                                "Minta PIN setiap kali membuka app. Biometric bisa ditambah nanti lewat library sistem.",
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = lockEnabled,
                            onCheckedChange = { on ->
                                if (on) {
                                    showPinSetup = true
                                    pin1 = ""
                                    pin2 = ""
                                    pinError = ""
                                } else {
                                    lockEnabled = false
                                    AppPrefs.clearPin(context)
                                    showPinSetup = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Ink,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Muted
                            )
                        )
                    }

                    if (showPinSetup || (lockEnabled && AppPrefs.pinHash(context).isEmpty())) {
                        Spacer(Modifier.height(10.dp))
                        Text("Atur PIN (4–6 digit)", fontWeight = FontWeight.Medium, color = Ink, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = pin1,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin1 = it },
                            label = { Text("PIN baru") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = pin2,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin2 = it },
                            label = { Text("Ulangi PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (pinError.isNotEmpty()) {
                            Text(pinError, color = Color(0xFFD32F2F), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                when {
                                    pin1.length !in 4..6 -> pinError = "PIN harus 4–6 digit"
                                    pin1 != pin2 -> pinError = "PIN tidak sama"
                                    else -> {
                                        AppPrefs.setPin(context, pin1)
                                        AppPrefs.setAppLockEnabled(context, true)
                                        lockEnabled = true
                                        showPinSetup = false
                                        pin1 = ""
                                        pin2 = ""
                                        pinError = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Ink)
                        ) {
                            Text("Simpan PIN")
                        }
                    } else if (lockEnabled) {
                        TextButton(onClick = {
                            showPinSetup = true
                            pin1 = ""
                            pin2 = ""
                            pinError = ""
                        }) {
                            Text("Ganti PIN", color = Ink)
                        }
                    }
                }
            }

            item {
                SettingsSection(icon = Icons.Filled.Schedule, title = "Jadwal otomatis") {
                    Text(
                        "Batas mengejar jadwal terlewat (hari). Mencegah ratusan transaksi retroaktif jika app lama tidak dibuka.",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AppPrefs.CATCHUP_OPTIONS.size) { idx ->
                            val d = AppPrefs.CATCHUP_OPTIONS[idx]
                            Chip(
                                label = "$d hari",
                                selected = catchupDays == d
                            ) {
                                catchupDays = d
                                AppPrefs.setMaxCatchupDays(context, d)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onAddSalary,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink)
                    ) {
                        Text("Tambah jadwal gaji")
                    }
                }
            }

            item {
                SettingsSection(icon = Icons.Filled.Backup, title = "Backup & restore") {
                    Text(
                        "Ekspor/impor data keuangan. Saat ini file JSON belum terenkripsi — jangan bagikan ke sembarang orang.",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Button(
                        onClick = onOpenBackup,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink)
                    ) {
                        Text("Buka Backup & Restore")
                    }
                }
            }

            item {
                SettingsSection(icon = Icons.Filled.Tune, title = "Tentang data") {
                    Text(
                        "Skala uang: 1 unit = Rp1 (rupiah utuh, bukan sen).\n" +
                            "Versi app: 1.0, Room schema v9",
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
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

private fun exampleAmount(prefix: Int, zeros: Int): String {
    if (zeros <= 0) return "Rp$prefix (exact)"
    val full = prefix.toString() + "0".repeat(zeros)
    return try {
        "Rp" + NumberFormat.getInstance(Locale("id", "ID")).format(full.toLong())
    } catch (_: Exception) {
        "Rp$full"
    }
}

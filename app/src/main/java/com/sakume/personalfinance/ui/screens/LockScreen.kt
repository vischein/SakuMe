package com.sakume.personalfinance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sakume.personalfinance.ui.AppLock
import com.sakume.personalfinance.ui.PinResult
import com.sakume.personalfinance.ui.findFragmentActivity
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Paper
import com.sakume.personalfinance.ui.theme.Red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Layar kunci penuh: PIN 6 digit + tombol biometrik (bila diaktifkan dan tersedia). */
@Composable
fun LockScreen() {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lockoutMs by remember { mutableStateOf(AppLock.lockoutRemainingMs(context)) }
    var checking by remember { mutableStateOf(false) }
    val pinLength = remember { AppLock.pinLength(context) }
    val biometricReady = remember { AppLock.biometricEnabled(context) && AppLock.biometricAvailable(context) }

    fun promptBiometric() {
        activity?.let { AppLock.promptBiometric(it) { AppLock.unlock(context) } }
    }

    LaunchedEffect(Unit) {
        if (biometricReady && lockoutMs == 0L) promptBiometric()
    }
    LaunchedEffect(lockoutMs > 0L) {
        while (lockoutMs > 0L) {
            delay(500)
            lockoutMs = AppLock.lockoutRemainingMs(context)
        }
    }

    fun submit(entered: String) {
        checking = true
        scope.launch {
            val result = withContext(Dispatchers.Default) { AppLock.verify(context, entered) }
            checking = false
            pin = ""
            when (result) {
                PinResult.Success -> AppLock.unlock(context)
                is PinResult.Wrong -> error = "PIN salah. Sisa ${result.attemptsLeft} percobaan."
                is PinResult.Locked -> {
                    lockoutMs = result.remainingMs
                    error = null
                }
            }
        }
    }

    val message = when {
        lockoutMs > 0L -> "Terlalu banyak percobaan. Coba lagi dalam ${(lockoutMs + 999) / 1000} detik."
        else -> error
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("SakuMe", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("Masukkan PIN untuk membuka", fontSize = 14.sp, color = Muted)
            Spacer(Modifier.height(28.dp))
            PinDots(pin.length, pinLength)
            Spacer(Modifier.height(14.dp))
            Text(
                message.orEmpty(),
                color = Red,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(36.dp)
            )
            Spacer(Modifier.height(10.dp))
            PinPad(
                enabled = !checking && lockoutMs == 0L,
                onDigit = { d ->
                    if (pin.length < pinLength) {
                        error = null
                        pin += d
                        if (pin.length == pinLength) submit(pin)
                    }
                },
                onBackspace = { pin = pin.dropLast(1) },
                leftSlot = if (biometricReady) {
                    {
                        PinKey(onClick = { promptBiometric() }) {
                            Icon(Icons.Filled.Fingerprint, "Gunakan biometrik", tint = Ink, modifier = Modifier.size(30.dp))
                        }
                    }
                } else null
            )
        }
    }
}

/** Dialog layar penuh untuk memasukkan PIN (atur PIN baru, ulangi PIN, verifikasi PIN lama). */
@Composable
fun PinEntryDialog(
    title: String,
    subtitle: String,
    error: String?,
    length: Int,
    onDismiss: () -> Unit,
    onComplete: (String) -> Unit
) {
    var pin by remember(title) { mutableStateOf("") }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Tutup", tint = Ink) }
                }
                Spacer(Modifier.weight(1f))
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(subtitle, fontSize = 14.sp, color = Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(28.dp))
                PinDots(pin.length, length)
                Spacer(Modifier.height(14.dp))
                Text(
                    error.orEmpty(),
                    color = Red,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.height(36.dp)
                )
                Spacer(Modifier.height(10.dp))
                PinPad(
                    enabled = true,
                    onDigit = { d ->
                        if (pin.length < length) {
                            pin += d
                            if (pin.length == length) {
                                val entered = pin
                                pin = ""
                                onComplete(entered)
                            }
                        }
                    },
                    onBackspace = { pin = pin.dropLast(1) }
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PinDots(filled: Int, length: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(length) { i ->
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .then(
                        if (i < filled) Modifier.background(Ink)
                        else Modifier.border(1.5.dp, Muted, CircleShape)
                    )
            )
        }
    }
}

@Composable
private fun PinPad(
    enabled: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    leftSlot: (@Composable () -> Unit)? = null
) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { d ->
                    PinKey(onClick = { if (enabled) onDigit(d) }) {
                        Text(d, fontSize = 26.sp, fontWeight = FontWeight.Medium, color = Ink)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            if (leftSlot != null) leftSlot() else Spacer(Modifier.size(72.dp))
            PinKey(onClick = { if (enabled) onDigit("0") }) {
                Text("0", fontSize = 26.sp, fontWeight = FontWeight.Medium, color = Ink)
            }
            PinKey(onClick = onBackspace, plain = true) {
                Icon(Icons.AutoMirrored.Filled.Backspace, "Hapus", tint = Ink, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun PinKey(onClick: () -> Unit, plain: Boolean = false, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (plain) Color.Transparent else Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

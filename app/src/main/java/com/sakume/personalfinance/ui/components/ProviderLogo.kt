package com.sakume.personalfinance.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Red

/**
 * Logo provider tanpa “box luar” ekstra.
 * Jika drawable ada: gambar mengisi penuh ukuran [size] (Crop).
 * Jika tidak: inisial + warna brand.
 */
@Composable
fun ProviderLogo(
    provider: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    overdue: Boolean = false,
    corner: Dp = 14.dp
) {
    val style = remember(provider) { providerStyle(provider) }
    val resId = providerLogoRes(LocalContext.current, style.drawableName)
    val shape = RoundedCornerShape(corner)
    val borderColor = when {
        overdue -> Red.copy(alpha = 0.35f)
        resId != 0 -> Color.Transparent
        else -> Ink.copy(alpha = 0.08f)
    }

    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(if (resId != 0) Color.Transparent else style.bg)
            .border(0.5.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(resId),
                contentDescription = provider,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                style.initials,
                color = style.fg,
                fontSize = if (style.initials.length <= 2) 15.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@DrawableRes
private fun providerLogoRes(context: android.content.Context, drawableName: String?): Int {
    if (drawableName.isNullOrBlank()) return 0
    return context.resources.getIdentifier(drawableName, "drawable", context.packageName)
}

data class ProviderStyle(
    val initials: String,
    val bg: Color,
    val fg: Color,
    val drawableName: String? = null
)

fun providerStyle(provider: String): ProviderStyle {
    val key = provider.trim().lowercase().replace(" ", "")
    return when {
        key.contains("spaylater") || key == "spay" || key.contains("shopeepaylater") ->
            ProviderStyle("SP", Color(0xFFEE4D2D).copy(alpha = 0.12f), Color(0xFFEE4D2D), "ic_provider_spaylater")
        key.contains("spinjam") || key.contains("shopeepinjam") ->
            ProviderStyle("SJ", Color(0xFFEE4D2D).copy(alpha = 0.12f), Color(0xFFEE4D2D), "ic_provider_spinjam")
        key.contains("kredivo") ->
            ProviderStyle("KD", Color(0xFF1A73E8).copy(alpha = 0.12f), Color(0xFF1A73E8), "ic_provider_kredivo")
        key.contains("akulaku") ->
            ProviderStyle("AK", Color(0xFFFF6A00).copy(alpha = 0.12f), Color(0xFFFF6A00), "ic_provider_akulaku")
        key.contains("gopay") || key.contains("gopaylater") || key == "gopay" ->
            ProviderStyle("GP", Color(0xFF00AED6).copy(alpha = 0.12f), Color(0xFF008CBA), "ic_provider_gopay")
        key.contains("homecredit") || (key.contains("home") && key.contains("credit")) ->
            ProviderStyle("HC", Color(0xFFED1C24).copy(alpha = 0.12f), Color(0xFFED1C24), "ic_provider_homecredit")
        key.contains("indodana") ->
            ProviderStyle("ID", Color(0xFF4143B2).copy(alpha = 0.12f), Color(0xFF4143B2), "ic_provider_indodana")
        key.contains("lazada") ->
            ProviderStyle("LZ", Color(0xFF0F146D).copy(alpha = 0.12f), Color(0xFF0F146D), "ic_provider_lazada")
        key.contains("tokopedia") || key.contains("toped") ->
            ProviderStyle("TP", Color(0xFF42B549).copy(alpha = 0.12f), Color(0xFF42B549), "ic_provider_tokopedia")
        else -> {
            val initials = providerInitials(provider)
            ProviderStyle(initials, Ink.copy(alpha = 0.06f), Ink, null)
        }
    }
}

private fun providerInitials(provider: String): String {
    val parts = provider.trim().split(Regex("\\s+|[-_]")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts[0].first()}${parts[1].first()}".uppercase()
    }
}

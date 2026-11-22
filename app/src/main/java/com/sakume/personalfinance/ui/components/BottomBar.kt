package com.sakume.personalfinance.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Paper

private data class BottomNavItem(
    val key: String,
    val label: String,
    val filled: ImageVector,
    val outlined: ImageVector,
)

/** Aksi cepat di menu "+". [key] = nilai `dialog` di FinanceApp. Hapus/ubah item di sini untuk menyesuaikan menu. */
private data class QuickAction(val key: String, val label: String, val icon: ImageVector)

private val quickActions = listOf(
    QuickAction("transaction", "Transaksi", Icons.Filled.Add),
    QuickAction("transfer", "Transfer", Icons.Filled.SwapHoriz),
    QuickAction("bill", "Tagihan", Icons.AutoMirrored.Filled.ReceiptLong),
    QuickAction("investment", "Investasi", Icons.AutoMirrored.Filled.TrendingUp),
    QuickAction("goal", "Tabungan", Icons.Filled.Savings),
    QuickAction("debt", "Piutang / Utang", Icons.Filled.SwapVert),
)

/**
 * Bottom bar: Home · Analisis · [ + ] · Bills · Invest(Tabungan/Investasi/Piutang).
 * Tombol "+" di tengah membuka menu aksi cepat; pilihan dikirim lewat [onQuickAction] (key dialog).
 */
@Composable
fun SakuMeBottomBar(
    selected: String,
    onSelect: (String) -> Unit,
    investSegment: Int = 0,
    onQuickAction: (String) -> Unit = {},
) {
    val navItems =
        listOf(
            BottomNavItem("Home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem("Analisis", "Analisis", Icons.Filled.Insights, Icons.Outlined.Insights),
            BottomNavItem(
                key = "Bills",
                label = "Bills",
                filled = Icons.AutoMirrored.Filled.ReceiptLong,
                outlined = Icons.AutoMirrored.Outlined.ReceiptLong,
            ),
            // Tab Invest berganti label/ikon mengikuti segmen: Tabungan · Investasi · Piutang/Utang
            when (investSegment) {
                0 -> BottomNavItem("Invest", "Tabungan", Icons.Filled.Savings, Icons.Outlined.Savings)
                1 -> BottomNavItem(
                    "Invest", "Investasi",
                    Icons.AutoMirrored.Filled.TrendingUp, Icons.AutoMirrored.Outlined.TrendingUp,
                )
                else -> BottomNavItem("Invest", "Piutang", Icons.Filled.SwapVert, Icons.Outlined.SwapVert)
            },
        )
    var menuOpen by remember { mutableStateOf(false) }

    NavigationBar(
        containerColor = CardBg,
        tonalElevation = 0.dp,
        modifier = Modifier.border(1.dp, Color(0xFFEEEEEE)),
    ) {
        navItems.take(2).forEach { item ->
            NavItem(item, selected == item.key) { onSelect(item.key) }
        }
        QuickAddSlot(
            open = menuOpen,
            onOpenChange = { menuOpen = it },
            onAction = onQuickAction,
        )
        navItems.drop(2).forEach { item ->
            NavItem(item, selected == item.key) { onSelect(item.key) }
        }
    }
}

@Composable
private fun RowScope.NavItem(item: BottomNavItem, isSelected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = if (isSelected) item.filled else item.outlined,
                contentDescription = item.label,
            )
        },
        label = {
            Text(
                text = item.label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Ink,
                indicatorColor = Ink,
                unselectedIconColor = Muted,
                unselectedTextColor = Muted,
            ),
    )
}

@Composable
private fun RowScope.QuickAddSlot(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onAction: (String) -> Unit,
) {
    val density = LocalDensity.current
    var slotHeightPx by remember { mutableStateOf(0) }
    val rotation by animateFloatAsState(
        targetValue = if (open) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "plusRotation",
    )
    Box(
        Modifier
            .weight(0.9f)
            // Tinggi NavigationBar hanya batas minimum; jangan isi penuh tinggi, nanti slot melebar setinggi layar.
            .heightIn(min = 80.dp)
            .onSizeChanged { slotHeightPx = it.height },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .offset(y = (-2).dp)
                .size(54.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = Ink.copy(alpha = 0.25f),
                    spotColor = Ink.copy(alpha = 0.40f),
                )
                .clip(CircleShape)
                .background(Ink)
                .clickable(
                    onClickLabel = if (open) "Tutup menu tambah" else "Buka menu tambah",
                    role = Role.Button,
                    onClick = { onOpenChange(!open) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Tambah",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { rotationZ = rotation },
            )
        }
        if (open) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(0, -(slotHeightPx + with(density) { 10.dp.roundToPx() })),
                onDismissRequest = { onOpenChange(false) },
                properties = PopupProperties(focusable = true),
            ) {
                QuickMenu { key ->
                    onOpenChange(false)
                    onAction(key)
                }
            }
        }
    }
}

@Composable
private fun QuickMenu(onPick: (String) -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    val shape = RoundedCornerShape(24.dp)
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(160)) +
            scaleIn(
                initialScale = 0.88f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
            ) +
            slideInVertically(tween(200)) { it / 8 },
    ) {
        Column(
            Modifier
                .width(304.dp)
                .clip(shape)
                .background(CardBg)
                .border(1.dp, Ink.copy(alpha = 0.10f), shape)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Tambah baru",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
            quickActions.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { action ->
                        QuickTile(action, Modifier.weight(1f)) { onPick(action.key) }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickTile(action: QuickAction, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .clip(shape)
            .background(Paper)
            .clickable(onClickLabel = "Tambah ${action.label}", role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).background(Ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(action.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            action.label,
            color = Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

package com.sakume.personalfinance.ui.components

import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted

private data class BottomNavItem(
    val key: String,
    val label: String,
    val filled: ImageVector,
    val outlined: ImageVector,
)

@Composable
fun SakuMeBottomBar(
    selected: String,
    onSelect: (String) -> Unit,
    investSegment: Int = 0,
) {
    val investIsSavings = investSegment == 0
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
            BottomNavItem(
                key = "Invest",
                label = if (investIsSavings) "Tabungan" else "Investasi",
                filled = if (investIsSavings) Icons.Filled.Savings else Icons.AutoMirrored.Filled.TrendingUp,
                outlined = if (investIsSavings) Icons.Outlined.Savings else Icons.AutoMirrored.Outlined.TrendingUp,
            ),
            BottomNavItem("More", "More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz),
        )
    NavigationBar(
        containerColor = CardBg,
        tonalElevation = 0.dp,
        modifier = Modifier.border(1.dp, Color(0xFFEEEEEE)),
    ) {
        navItems.forEach { item ->
            val isSelected = selected == item.key
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(item.key) },
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
    }
}

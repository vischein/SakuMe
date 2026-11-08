package com.sakume.personalfinance.ui.components

import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted

@Composable
fun SakuMeBottomBar(selected: String, onSelect: (String) -> Unit) {
    val navItems = listOf(
        Triple("Home", Icons.Filled.Home, Icons.Outlined.Home),
        Triple("Accounts", Icons.Filled.CreditCard, Icons.Outlined.CreditCard),
        Triple("Bills", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
        Triple("Invest", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
        Triple("More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
    )
    NavigationBar(
        containerColor = CardBg,
        tonalElevation = 0.dp,
        modifier = Modifier.border(1.dp, Color(0xFFEEEEEE))
    ) {
        navItems.forEach { (label, filled, outlined) ->
            val isSelected = selected == label
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(label) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) filled else outlined,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Ink,
                    indicatorColor = Ink,
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted
                )
            )
        }
    }
}

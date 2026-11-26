package com.sakume.personalfinance.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.sakume.personalfinance.data.CategoryEntity

val LocalCategories = compositionLocalOf<Map<String, CategoryEntity>> { emptyMap() }

private val ICONS: Map<String, ImageVector> = linkedMapOf(
    "restaurant" to Icons.Filled.Restaurant,
    "cafe" to Icons.Filled.LocalCafe,
    "car" to Icons.Filled.DirectionsCar,
    "fuel" to Icons.Filled.LocalGasStation,
    "cart" to Icons.Filled.ShoppingCart,
    "clothes" to Icons.Filled.Checkroom,
    "smoke" to Icons.Filled.SmokingRooms,
    "movie" to Icons.Filled.Movie,
    "medical" to Icons.Filled.MedicalServices,
    "fitness" to Icons.Filled.FitnessCenter,
    "phone" to Icons.Filled.PhoneAndroid,
    "home" to Icons.Filled.Home,
    "bolt" to Icons.Filled.Bolt,
    "school" to Icons.Filled.School,
    "cash" to Icons.Filled.Payments,
    "gift" to Icons.Filled.CardGiftcard,
    "pets" to Icons.Filled.Pets,
    "flight" to Icons.Filled.Flight,
    "build" to Icons.Filled.Build,
    "work" to Icons.Filled.Work,
    "receipt" to Icons.Filled.ReceiptLong,
    "bank" to Icons.Filled.AccountBalance,
    "trend" to Icons.Filled.TrendingUp,
    "savings" to Icons.Filled.Savings,
    "debt" to Icons.Filled.AccountBalanceWallet,
    "swap" to Icons.Filled.SwapHoriz,
    "category" to Icons.Filled.Category
)

val PICKABLE_CATEGORY_ICONS: List<String> =
    ICONS.keys.filter { it !in setOf("receipt", "bank", "trend", "savings", "debt", "swap", "category", "work") }

val CATEGORY_PALETTE: List<Long> = listOf(
    0xFFE8A838, 0xFF3B82F6, 0xFFF97316, 0xFFEC4899, 0xFF8B5CF6, 0xFFEF4444,
    0xFF06B6D4, 0xFF14B8A6, 0xFFEAB308, 0xFF6366F1, 0xFF22C55E, 0xFF78716C
)

fun categoryIconVector(key: String): ImageVector = ICONS[key] ?: Icons.Filled.Category

fun categoryColor(category: CategoryEntity?): Color = Color(category?.color ?: 0xFF6B7280)

fun pickableCategories(all: List<CategoryEntity>, txType: String): List<CategoryEntity> =
    all.filter { it.accepts(txType) }.sortedBy { it.sortOrder }

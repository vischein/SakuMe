package com.sakume.personalfinance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.TransactionEntity
import com.sakume.personalfinance.ui.displayCategory
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.PanelSoft
import com.sakume.personalfinance.ui.theme.Green
import com.sakume.personalfinance.ui.theme.GreenSoft
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Red
import com.sakume.personalfinance.ui.theme.RedSoft
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp

@Composable
fun EmptyHint(text: String) {
    Text(
        text = text,
        color = Muted,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(18.dp)
    )
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

@Composable
fun StatTile(
    title: String,
    value: String,
    icon: ImageVector,
    bg: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).background(tint.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Muted, fontSize = 12.sp)
            Text(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun TransactionRow(
    t: TransactionEntity,
    accountName: String,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onToggleSelect: (() -> Unit)? = null
) {
    val isTransfer = t.type == "transfer_in" || t.type == "transfer_out"
    val isIn = t.type == "income" || t.type == "transfer_in"
    val tint = when {
        isTransfer -> Color(0xFFE8A838)
        isIn -> Green
        else -> Red
    }
    val soft = when {
        isTransfer -> Color(0xFFFFF4DF)
        isIn -> GreenSoft
        else -> RedSoft
    }
    val sign = when {
        isTransfer && t.type == "transfer_out" -> "→"
        isTransfer && t.type == "transfer_in" -> "←"
        isIn -> "+"
        else -> "-"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFFE8E8E8) else CardBg)
            .then(
                if (selectionMode && onToggleSelect != null) Modifier.clickable(onClick = onToggleSelect)
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) Ink else Color.White)
                    .border(1.5.dp, if (selected) Ink else Color(0xFFCCCCCC), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
        }
        Box(
            Modifier.size(40.dp).background(soft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when {
                    isTransfer -> Icons.Filled.SwapHoriz
                    isIn -> Icons.Filled.SouthWest
                    else -> Icons.Filled.NorthEast
                },
                null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                displayCategory(
                    t.note.ifBlank { t.category }.ifBlank { if (isIn) "Pemasukan" else "Pengeluaran" }
                ),
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "$accountName · ${formatDate(t.dateMillis)}",
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            "$sign${rupiah(t.amount)}",
            fontWeight = FontWeight.Bold,
            color = tint,
            fontSize = 14.sp
        )
    }
}

@Composable
fun QuickAction(
    icon: ImageVector,
    label: String,
    bgColor: Color = PanelSoft,
    iconTint: Color = Color.White,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier.size(50.dp).background(bgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = textColor, fontSize = 12.sp)
    }
}

@Composable
fun MetricChip(title: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(com.sakume.personalfinance.ui.theme.PanelSoft)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(22.dp).background(tint.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (title.contains("Masuk")) Icons.Filled.SouthWest else Icons.Filled.NorthEast,
                    null,
                    tint = tint,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(title, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Ink else Color.White
    val fg = if (selected) Color.White else Ink
    Text(
        text = label,
        color = fg,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
fun SegmentedChips(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    minSegmentWidth: Dp = 84.dp,
    onSelect: (Int) -> Unit
) {
    if (options.isEmpty()) return
    val count = options.size
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val segW = maxOf(maxWidth / count, minSegmentWidth)
        val offsetX by animateDpAsState(
            targetValue = segW * selectedIndex.coerceIn(0, count - 1),
            label = "segmentOffset"
        )
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .horizontalScroll(rememberScrollState())
        ) {
            Box(Modifier.width(segW * count).height(44.dp)) {
                if (selectedIndex in 0 until count) {
                    Box(
                        Modifier
                        .offset(x = offsetX)
                        .width(segW)
                        .fillMaxHeight()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Ink)
                    )
                }
                Row(Modifier.fillMaxSize()) {
                    options.forEachIndexed { i, label ->
                        val selected = i == selectedIndex
                        Box(
                            Modifier
                                .width(segW)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onSelect(i) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else Ink,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

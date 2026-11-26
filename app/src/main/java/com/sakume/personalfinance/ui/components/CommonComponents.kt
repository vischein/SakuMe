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
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import com.sakume.personalfinance.ui.formatTime
import com.sakume.personalfinance.ui.isStartOfDay
import com.sakume.personalfinance.ui.LocalCategories
import com.sakume.personalfinance.ui.categoryIconVector
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
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
import com.sakume.personalfinance.ui.theme.CancelBg
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign

internal const val HIDDEN_AMOUNT = "Rp******"

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
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    collapsible: Boolean = false,
    expanded: Boolean = true,
    summary: String? = null,
    onToggle: (() -> Unit)? = null
) {
    val toggle: (() -> Unit)? = if (collapsible) onToggle else null
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "sectionChevron"
    )
    val toggleModifier = if (toggle != null) {
        Modifier
            .clickable(
                onClickLabel = if (expanded) "Ciutkan bagian $title" else "Buka bagian $title",
                role = Role.Button,
                onClick = toggle
            )
            .semantics { stateDescription = if (expanded) "Terbuka" else "Tertutup" }
    } else Modifier

    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(toggleModifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (toggle != null) {
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = chevron }
                    )
                }
            }
            AnimatedVisibility(
                visible = toggle != null && !expanded && !summary.isNullOrBlank(),
                enter = fadeIn(tween(250)) + expandVertically(tween(300, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(300, easing = FastOutSlowInEasing))
            ) {
                Text(
                    text = summary.orEmpty(),
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClickLabel = "$action $title", onClick = onAction)
                    .padding(start = 10.dp, top = 8.dp, bottom = 8.dp)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    t: TransactionEntity,
    accountName: String,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    grouped: Boolean = false,
    showDate: Boolean = true,
    onLongPress: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    hideAmount: Boolean = false,
    selectionShape: Shape? = null,
    selectedAbove: Boolean = false
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
    val isTransferDisplay = isTransfer || t.note == "Transfer"
    val category = LocalCategories.current[t.categoryId]
    val title = displayCategory(
        t.note.ifBlank { if (isTransferDisplay) t.category else category?.name.orEmpty() }
            .ifBlank { if (isIn) "Pemasukan" else "Pengeluaran" }
    )
    val icon = remember(isTransferDisplay, category?.icon) {
        if (isTransferDisplay) Icons.Filled.SwapHoriz
        else category?.let { categoryIconVector(it.icon) }
            ?: if (isIn) Icons.Filled.SouthWest else Icons.Filled.NorthEast
    }
    val categoryLabel = if (!isTransferDisplay && t.note.isNotBlank()) category?.name else null
    val subtitle = remember(accountName, t.dateMillis, showDate, categoryLabel) {
        val time = if (isStartOfDay(t.dateMillis)) null else formatTime(t.dateMillis)
        listOfNotNull(categoryLabel, accountName, if (showDate) formatDate(t.dateMillis) else null, time).joinToString(" · ")
    }

    val interactive = selectionMode && onToggleSelect != null
    val tapModifier = when {
        interactive -> Modifier.combinedClickable(
            onClickLabel = if (selected) "Batalkan pilihan" else "Pilih transaksi",
            role = Role.Checkbox,
            onLongClick = onLongPress,
            onClick = onToggleSelect!!
        )
        onLongPress != null || onClick != null -> Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onLongClickLabel = "Pilih transaksi",
            onLongClick = onLongPress,
            onClick = { onClick?.invoke() }
        )
        else -> Modifier
    }
    val borderColor by animateColorAsState(
        if (selected) Ink else Color.Transparent,
        tween(160),
        label = "txRowBorder"
    )

    Row(
        modifier
            .fillMaxWidth()
            .then(if (grouped) Modifier else Modifier.clip(RoundedCornerShape(14.dp)).background(CardBg))
            .then(tapModifier)
            .then(if (selectionShape == null) Modifier.padding(horizontal = 4.dp, vertical = 2.dp) else Modifier)
            .then(
                if (selectionShape == null) Modifier.border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                else Modifier.drawBehind {
                    if (borderColor.alpha == 0f) return@drawBehind
                    val sw = 1.5.dp.toPx()
                    val outline = selectionShape.createOutline(
                        Size(size.width - sw, size.height - sw), layoutDirection, this
                    )
                    clipRect(top = if (selectedAbove) sw else 0f) {
                        translate(sw / 2, sw / 2) {
                            drawOutline(outline, borderColor, style = Stroke(width = sw))
                        }
                    }
                }
            )
            .padding(
                horizontal = if (selectionShape == null) 8.dp else 12.dp,
                vertical = if (selectionShape == null) (if (grouped) 10.dp else 8.dp) else 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).background(soft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "$sign${if (hideAmount) HIDDEN_AMOUNT else rupiah(t.amount)}",
            fontWeight = FontWeight.Bold,
            color = tint,
            fontSize = 14.sp,
            maxLines = 1,
            softWrap = false
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

private val ChipTrack = CancelBg
private val ChipHairline = Ink.copy(alpha = 0.0f)
private val ChipPill = RoundedCornerShape(50)

private val SegmentInset = 0.dp
private val SegmentHeight = 44.dp
private val SegmentTextPad = 14.dp
private val SegmentMaxWidth = 220.dp
private val SegmentIconSize = 16.dp
private val SegmentGap = 6.dp

@Composable
private fun rememberPressScale(source: InteractionSource, pressedScale: Float = 0.96f): State<Float> {
    val pressed by source.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "chipPressScale"
    )
}

private fun badgeLabel(n: Int) = if (n > 99) "99+" else n.toString()

@Composable
private fun CountBadge(count: Int, selected: Boolean) {
    val bg by animateColorAsState(
        if (selected) Ink else Ink.copy(alpha = 0.10f),
        tween(220),
        label = "badgeBg"
    )
    val fg by animateColorAsState(
        if (selected) Color.White else Muted,
        tween(220),
        label = "badgeFg"
    )
    Box(
        Modifier
            .height(18.dp)
            .widthIn(min = 18.dp)
            .background(bg, ChipPill)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(badgeLabel(count), color = fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private class SegMetric(val text: Dp, val natural: Dp)

@Composable
fun SegmentedChips(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    minSegmentWidth: Dp = 84.dp,
    leadingIcons: List<ImageVector?>? = null,
    badges: List<Int?>? = null,
    onSelect: (Int) -> Unit
) {
    if (options.isEmpty()) return
    val count = options.size
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val trackWidth = maxWidth
        val innerAvail = maxOf(trackWidth - SegmentInset * 2, 0.dp)

        val metrics = remember(options, leadingIcons, badges, labelStyle, density) {
            options.mapIndexed { i, label ->
                val textPx = measurer.measure(text = label, style = labelStyle, maxLines = 1).size.width
                val textDp = with(density) { textPx.toDp() }
                var w = textDp + SegmentTextPad * 2
                if (leadingIcons?.getOrNull(i) != null) w += SegmentIconSize + SegmentGap
                badges?.getOrNull(i)?.let { n ->
                    w += maxOf(18.dp, 8.dp + 6.dp * badgeLabel(n).length) + SegmentGap
                }
                SegMetric(text = textDp, natural = minOf(w, SegmentMaxWidth))
            }
        }

        val equalW = maxOf(innerAvail / count, minSegmentWidth)
        val widths: List<Dp> = if (metrics.all { it.natural <= equalW }) {
            List(count) { equalW }
        } else {
            val base = metrics.map { maxOf(it.natural, minSegmentWidth) }
            val total = base.fold(0.dp) { a, b -> a + b }
            if (total < innerAvail) {
                val extra = (innerAvail - total) / count
                base.map { it + extra }
            } else base
        }
        val starts = remember(widths) {
            var acc = 0.dp
            widths.map { w -> val s = acc; acc += w; s }
        }
        val totalWidth = widths.fold(0.dp) { a, b -> a + b }

        val hasSelection = selectedIndex in 0 until count
        val sel = selectedIndex.coerceIn(0, count - 1)
        val slide = spring<Dp>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
        val indicatorX by animateDpAsState(starts[sel], slide, label = "segmentX")
        val indicatorW by animateDpAsState(widths[sel], slide, label = "segmentW")
        val indicatorAlpha by animateFloatAsState(
            if (hasSelection) 1f else 0f,
            tween(200),
            label = "segmentAlpha"
        )

        val scrollState = rememberScrollState()
        val firstScroll = remember { mutableStateOf(true) }
        LaunchedEffect(selectedIndex, widths, scrollState.maxValue) {
            if (!hasSelection) return@LaunchedEffect
            val target = with(density) {
                (SegmentInset + starts[sel] + widths[sel] / 2 - trackWidth / 2).roundToPx()
            }
            if (firstScroll.value) {
                scrollState.scrollTo(target)
                if (scrollState.maxValue > 0 || target <= 0) firstScroll.value = false
            } else {
                scrollState.animateScrollTo(target)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(SegmentHeight)
                .clip(ChipPill)
                .background(ChipTrack)
                .horizontalScroll(scrollState)
        ) {
            Box(
                Modifier
                    .padding(SegmentInset)
                    .width(totalWidth)
                    .fillMaxHeight()
            ) {
                Box(
                    Modifier
                        .offset(x = indicatorX)
                        .width(indicatorW)
                        .fillMaxHeight()
                        .alpha(indicatorAlpha)
                        .shadow(
                            elevation = 2.5.dp,
                            shape = ChipPill,
                            clip = false,
                            ambientColor = Ink.copy(alpha = 0.10f),
                            spotColor = Ink.copy(alpha = 0.18f)
                        )
                        .background(Color.White, ChipPill)
                        .border(0.5.dp, ChipHairline, ChipPill)
                )
                Row(Modifier.fillMaxSize()) {
                    options.forEachIndexed { i, label ->
                        SegmentItem(
                            label = label,
                            selected = i == selectedIndex,
                            width = widths[i],
                            textWidth = metrics[i].text,
                            icon = leadingIcons?.getOrNull(i),
                            badge = badges?.getOrNull(i),
                            onClick = { onSelect(i) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentItem(
    label: String,
    selected: Boolean,
    width: Dp,
    textWidth: Dp,
    icon: ImageVector?,
    badge: Int?,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val scale by rememberPressScale(source)
    val fg by animateColorAsState(
        if (selected) Ink else Muted,
        tween(220),
        label = "segmentText"
    )
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(SegmentIconSize))
                Spacer(Modifier.width(SegmentGap))
            }
            Text(
                text = label,
                color = fg,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                
                modifier = Modifier.widthIn(min = textWidth).weight(1f, fill = false)
            )
            if (badge != null) {
                Spacer(Modifier.width(SegmentGap))
                CountBadge(badge, selected)
            }
        }
    }
}

@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    contentDescription: String? = null
) {
    val source = remember { MutableInteractionSource() }
    val scale by rememberPressScale(source)
    val bg by animateColorAsState(if (selected) Ink else ChipTrack, tween(220), label = "chipBg")
    val fg by animateColorAsState(if (selected) Color.White else Muted, tween(220), label = "chipFg")
    val border by animateColorAsState(if (selected) Ink else ChipHairline, tween(220), label = "chipBorder")
    val labelSemantics = if (contentDescription != null) Modifier.clearAndSetSemantics { } else Modifier

    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .heightIn(min = 36.dp)
            .clip(ChipPill)
            .background(bg)
            .border(1.dp, border, ChipPill)
            .semantics {
                this.selected = selected
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(SegmentIconSize))
            Spacer(Modifier.width(SegmentGap))
        }
        Box(contentAlignment = Alignment.Center) {
            
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.alpha(0f).clearAndSetSemantics { }
            )
            Text(
                label,
                color = fg,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = labelSemantics
            )
        }
    }
}

@Composable
fun ActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    containerColor: Color = CardBg,
    contentColor: Color = Ink
) {
    val source = remember { MutableInteractionSource() }
    val scale by rememberPressScale(source)
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(
        if (pressed) Ink.copy(alpha = 0.07f).compositeOver(containerColor) else containerColor,
        tween(150),
        label = "actionChipBg"
    )
    val labelSemantics = if (contentDescription != null) Modifier.clearAndSetSemantics { } else Modifier

    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .heightIn(min = 38.dp)
            .clip(ChipPill)
            .background(bg)
            .border(1.dp, ChipHairline, ChipPill)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription }
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(SegmentIconSize))
            Spacer(Modifier.width(SegmentGap))
        }
        Text(
            label,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = labelSemantics
        )
    }
}

@Composable
fun MetricChip(
    title: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val glyph = icon ?: if (title.contains("Masuk", ignoreCase = true)) Icons.Filled.SouthWest else Icons.Filled.NorthEast
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(PanelSoft)
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), shape)
            .semantics(mergeDescendants = true) { }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(24.dp).background(tint.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(glyph, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
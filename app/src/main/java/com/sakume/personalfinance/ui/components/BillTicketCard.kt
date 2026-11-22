package com.sakume.personalfinance.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Red

private val TicketCorner = 22.dp
private val NotchRadius = 8.dp
private val DividerHeight = NotchRadius * 2

private data class TicketShape(
    val corner: Dp,
    val notchRadius: Dp,
    val notchY: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = with(density) { corner.toPx() }
        val nr = with(density) { notchRadius.toPx() }
        val base =
            Path().apply {
                addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r)))
            }
        if (notchY <= 0f || notchY >= size.height) return Outline.Generic(base)
        val cuts =
            Path().apply {
                addOval(Rect(Offset(-nr, notchY - nr), Size(nr * 2, nr * 2)))
                addOval(Rect(Offset(size.width - nr, notchY - nr), Size(nr * 2, nr * 2)))
            }
        return Outline.Generic(Path.combine(PathOperation.Difference, base, cuts))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BillTicketCard(
    provider: String,
    subtitle: String,
    dayLabel: String,
    overdue: Boolean,
    amountText: String,
    dueDateText: String?,
    modifier: Modifier = Modifier,
    installmentIndex: Int = 0,
    installmentTenor: Int = 0,
    amountLabel: String = "Total Tagihan",
    actionLabel: String = "Bayar",
    selecting: Boolean = false,
    selected: Boolean = false,
    onAction: () -> Unit,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val brand = remember(provider) { providerStyle(provider).fg }
    val tone = if (overdue) Red else brand
    val density = LocalDensity.current

    var topHeightPx by remember { mutableFloatStateOf(0f) }
    val notchY = topHeightPx + with(density) { DividerHeight.toPx() } / 2f
    val shape = remember(notchY) { TicketShape(TicketCorner, NotchRadius, notchY) }

    val clickModifier =
        if (onClick != null || onLongClick != null) {
            Modifier.combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick,
            )
        } else {
            Modifier
        }

    Column(
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.12f),
            ).clip(shape)
            .background(CardBg)
            .then(if (selected) Modifier.border(1.5.dp, Ink, shape) else Modifier)
            .then(clickModifier),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { topHeightPx = it.height.toFloat() }
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(tone.copy(alpha = 0.13f), Color.Transparent),
                            center = Offset(0f, 0f),
                            radius = size.width * 0.26f,
                        ),
                    )
                }.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProviderLogo(provider = provider, size = 50.dp, overdue = overdue, corner = 16.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    subtitle,
                    color = Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                if (selecting) {
                    TicketSelectCheck(selected)
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        if (!dueDateText.isNullOrBlank()) {
                            DueText(dueDateText, overdue)
                            Spacer(Modifier.height(3.dp))
                        }
                        DayPill(dayLabel, tone)
                    }
                }
            }

            if (installmentTenor > 1) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Cicilan ke-$installmentIndex dari $installmentTenor",
                        color = Ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$installmentIndex/$installmentTenor",
                        color = Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .background(Ink.copy(alpha = 0.06f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                TicketProgressBar(installmentIndex, installmentTenor, tone)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(DividerHeight)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            TicketDashedLine(Modifier.fillMaxWidth())
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    amountLabel,
                    color = Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
                Text(
                    amountText,
                    color = if (overdue) Red else Ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!selecting) {
                Spacer(Modifier.width(8.dp))
                ActionButton(label = actionLabel, tone = tone, onClick = onAction, provider = provider)
            }
        }
    }
}

@Composable
private fun DayPill(
    label: String,
    tone: Color,
) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(tone.copy(alpha = 0.07f))
            .border(1.dp, tone.copy(alpha = 0.22f), CircleShape)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Timer, contentDescription = null, tint = tone, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            color = tone,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun DueText(
    dateText: String,
    overdue: Boolean,
) {
    val fg = if (overdue) Red else Muted
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = fg, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            "Jatuh tempo: $dateText",
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    tone: Color,
    provider: String,
    onClick: () -> Unit,
) {
    val brush = Brush.horizontalGradient(listOf(tone, lerp(tone, Color.White, 0.18f)))
    Row(
        Modifier
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = tone.copy(alpha = 0.25f),
                spotColor = tone.copy(alpha = 0.40f),
            ).clip(CircleShape)
            .background(brush)
            .clickable(onClickLabel = "$label $provider", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun TicketProgressBar(
    index: Int,
    tenor: Int,
    tone: Color,
) {
    val fill = Brush.horizontalGradient(listOf(tone, lerp(tone, Color.White, 0.2f)))
    val track = Ink.copy(alpha = 0.08f)
    if (tenor <= 12) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(tenor) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .then(
                            if (i < index) {
                                Modifier.background(fill, CircleShape)
                            } else {
                                Modifier.background(track, CircleShape)
                            },
                        ),
                )
            }
        }
    } else {
        val progress = (index.toFloat() / tenor).coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(track),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(fill, CircleShape),
            )
        }
    }
}

@Composable
private fun TicketSelectCheck(selected: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) Ink else Color.Transparent)
            .border(1.5.dp, if (selected) Ink else Ink.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Terpilih", tint = Color.White, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
private fun TicketDashedLine(modifier: Modifier = Modifier) {
    val color = Ink.copy(alpha = 0.16f)
    Canvas(modifier.height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())),
        )
    }
}

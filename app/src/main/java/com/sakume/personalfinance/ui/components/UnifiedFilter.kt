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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Paper

data class FilterSection(
    val title: String,
    val options: List<String>,
    val selectedIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedFilterControl(
    title: String,
    summary: String,
    sections: List<FilterSection>,
    modifier: Modifier = Modifier,
    onApply: (List<Int>) -> Unit,
    onReset: (() -> Unit)? = null,
    resetLabel: String = "Reset",
    active: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .clickable(onClickLabel = "Atur filter") { expanded = true }
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "FILTER",
                color = Muted,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                title,
                color = Ink,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    summary,
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (active) Ink else Ink.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = "Atur filter",
                    tint = if (active) Color.White else Ink,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (active) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(CardBg, CircleShape)
                        .padding(2.dp)
                        .background(Color(0xFFFF3B30), CircleShape)
                )
            }
        }
    }

    if (expanded) {
        var draft by remember(expanded) { mutableStateOf(sections.map { it.selectedIndex }) }
        ModalBottomSheet(
            onDismissRequest = { expanded = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Paper,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 2.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Muted.copy(alpha = 0.35f))
                )
            }
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Filter", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Sesuaikan tampilan data", color = Muted, fontSize = 12.sp)
                        }
                        if (onReset != null) {
                            Text(
                                resetLabel,
                                color = Muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable {
                                        draft = sections.map { 0 }
                                    }
                                    .padding(horizontal = 9.dp, vertical = 7.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    sections.forEachIndexed { sectionIndex, section ->
                        Text(section.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        section.options.chunked(2).forEachIndexed { rowIndex, rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowOptions.forEachIndexed { colIndex, option ->
                                    val optionIndex = rowIndex * 2 + colIndex
                                    val selected = draft.getOrElse(sectionIndex) { section.selectedIndex } == optionIndex
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (selected) Ink.copy(alpha = 0.07f) else CardBg)
                                            .border(
                                                1.dp,
                                                if (selected) Ink.copy(alpha = 0.45f) else Ink.copy(alpha = 0.07f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                draft = draft.toMutableList().also { it[sectionIndex] = optionIndex }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            option,
                                            modifier = Modifier.weight(1f),
                                            color = Ink,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (selected) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = Ink, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                                if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(7.dp))
                        }
                        if (sectionIndex != sections.lastIndex) Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        if (onReset != null) {
                            Box(
                                modifier = Modifier.weight(1f).height(46.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(CardBg)
                                    .clickable {
                                        onReset()
                                        expanded = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Hapus filter", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f).height(46.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Ink)
                                .clickable {
                                    onApply(draft)
                                    expanded = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Terapkan filter", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
        }
    }
}
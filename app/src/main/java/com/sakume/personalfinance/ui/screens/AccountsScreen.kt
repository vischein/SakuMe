package com.sakume.personalfinance.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.TransactionEntity
import com.sakume.personalfinance.ui.components.EmptyHint
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.theme.Accent
import com.sakume.personalfinance.ui.theme.Paper
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.PanelSoft
import com.sakume.personalfinance.ui.theme.Red
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Edit
import com.sakume.personalfinance.ui.theme.CardBg

@Composable
fun ManageAccountsScreen(
    accounts: List<AccountEntity>,
    tx: List<TransactionEntity>,
    onAdd: () -> Unit,
    onTransfer: () -> Unit = {},
    onAddTx: () -> Unit,
    onAccountClick: (AccountEntity) -> Unit = {},
    onCash: () -> Unit = {},
    onRename: (AccountEntity) -> Unit = {},
    onDelete: (AccountEntity) -> Unit,
    onReorder: (List<String>) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val bankAccounts =
        accounts.filterNot {
            it.type.equals("Cash", true) ||
                it.name.equals("Dompet Tunai", true) ||
                it.name.equals("Cash", true)
        }
    var localList by remember { mutableStateOf(bankAccounts) }
    var mode by remember { mutableStateOf("normal") }
    var menuOpen by remember { mutableStateOf(false) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(accounts) {
        if (draggingId == null) localList = accounts
    }

    fun moveItem(
        from: Int,
        to: Int,
    ) {
        if (from == to || from !in localList.indices || to !in localList.indices) return
        val mutable = localList.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        localList = mutable
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Column {
                    Text("Rekening", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        when (mode) {
                            "reorder" -> "Geser kartu untuk mengubah urutan"
                            "rename" -> "Ketuk rekening untuk ubah nama"
                            "delete" -> "Ketuk ikon hapus pada rekening"
                            else -> "Kelola rekening & transfer"
                        },
                        color = Muted,
                        fontSize = 12.sp,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (mode != "normal") {
                    Button(
                        onClick = {
                            if (mode == "reorder") onReorder(localList.map { it.id })
                            mode = "normal"
                            draggingId = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text("Selesai", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Box {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Ink)
                            .clickable(onClick = { menuOpen = true }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Ubah urutan") },
                            onClick = {
                                mode = "reorder"
                                menuOpen = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Ubah nama rekening") },
                            onClick = {
                                mode = "rename"
                                menuOpen = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus rekening") },
                            onClick = {
                                mode = "delete"
                                menuOpen = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Tambah rekening") },
                            onClick = {
                                onAdd()
                                menuOpen = false
                            },
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompactAction(
                icon = Icons.Filled.Add,
                label = "Transaksi",
                bg = Ink,
                fg = Color.White,
                modifier = Modifier.weight(1f),
                onClick = onAddTx,
            )
            CompactAction(
                icon = Icons.Filled.Payments,
                label = "Tarik Tunai",
                bg = Color.White,
                fg = Ink,
                modifier = Modifier.weight(1f),
                onClick = onCash,
            )
            CompactAction(
                icon = Icons.Filled.SwapHoriz,
                label = "Transfer",
                bg = Ink,
                fg = Color.White,
                modifier = Modifier.weight(1f),
                onClick = onTransfer,
            )
        }

        if (localList.isEmpty()) {
            EmptyHint("Belum ada rekening.")
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp, top = 2.dp),
            ) {
                itemsIndexed(localList, key = { _, a -> a.id }) { index, a ->
                    val count = tx.count { it.accountId == a.id }
                    val isDragging = draggingId == a.id
                    AccountCard(
                        account = a,
                        txCount = count,
                        mode = mode,
                        isDragging = isDragging,
                        dragOffsetY = if (isDragging) dragOffsetY else 0f,
                        onDelete = { onDelete(a) },
                        onRename = { onRename(a) },
                        onClick = {
                            if (mode == "normal") {
                                onAccountClick(a)
                            } else if (mode == "rename") {
                                onRename(a)
                            }
                        },
                        onDragStart = {
                            draggingId = a.id
                            dragOffsetY = 0f
                        },
                        onDrag = { dy ->
                            dragOffsetY += dy
                            val itemH = 64f
                            val shift = (dragOffsetY / itemH).toInt()
                            val target = (index + shift).coerceIn(0, localList.lastIndex)
                            if (target != index) {
                                moveItem(index, target)
                                dragOffsetY -= (target - index) * itemH
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                val anim = Animatable(dragOffsetY)
                                anim.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                dragOffsetY = 0f
                                draggingId = null
                                onReorder(localList.map { it.id })
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactAction(
    icon: ImageVector,
    label: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                color = fg,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AccountCard(
    account: AccountEntity,
    txCount: Int,
    mode: String,
    isDragging: Boolean,
    dragOffsetY: Float,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val icon =
        when {
            account.type.equals("Cash", true) -> Icons.Filled.Payments
            account.type.equals("E-Wallet", true) -> Icons.Filled.PhoneAndroid
            else -> Icons.Filled.AccountBalance
        }
    Box(
        Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 2f else 0f)
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .then(if (isDragging) Modifier.shadow(10.dp, RoundedCornerShape(16.dp)) else Modifier)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .then(
                if (mode == "normal" || mode == "rename") Modifier.clickable(onClick = onClick) else Modifier,
            ).padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (mode == "reorder") {
                Icon(
                    Icons.Filled.DragHandle,
                    null,
                    tint = Muted,
                    modifier =
                        Modifier
                            .size(20.dp)
                            .pointerInput(account.id) {
                                detectDragGestures(
                                    onDragStart = { onDragStart() },
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragEnd() },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        onDrag(amount.y)
                                    },
                                )
                            },
                )
                Spacer(Modifier.width(8.dp))
            }
            Box(
                Modifier.size(42.dp).background(Paper, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Ink, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    account.name,
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    rupiah(account.balance),
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            if (mode == "rename") {
                IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Edit, null, tint = Ink, modifier = Modifier.size(20.dp))
                }
            } else if (mode == "delete") {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, null, tint = Red, modifier = Modifier.size(20.dp))
                }
            } else if (mode == "normal") {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        account.type.uppercase(),
                        color = Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$txCount trx",
                        color = Muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

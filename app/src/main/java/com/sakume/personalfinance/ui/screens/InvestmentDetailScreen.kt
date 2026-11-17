package com.sakume.personalfinance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.InvestmentEntity
import com.sakume.personalfinance.data.InvestmentHoldingEntity
import com.sakume.personalfinance.data.TransactionEntity
import com.sakume.personalfinance.ui.components.EmptyHint
import com.sakume.personalfinance.ui.components.SegmentedChips
import com.sakume.personalfinance.ui.formatDate
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Gold
import com.sakume.personalfinance.ui.theme.GoldSoft
import com.sakume.personalfinance.ui.theme.Green
import com.sakume.personalfinance.ui.theme.GreenSoft
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Paper
import com.sakume.personalfinance.ui.theme.Red
import com.sakume.personalfinance.ui.theme.RedSoft
import com.sakume.personalfinance.ui.transactionAccountLabel

private enum class HistoryKind { TOPUP, WITHDRAW, FEE }

private data class AssetHistory(
    val tx: TransactionEntity,
    val kind: HistoryKind,
    val withdrawSide: Boolean
)

@Composable
fun InvestmentDetailScreen(
    asset: InvestmentEntity,
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    holdings: List<InvestmentHoldingEntity>,
    onBack: () -> Unit
) {
    var filter by remember(asset.id) { mutableStateOf(0) }

    val available = remember(asset.value, holdings, asset.id) {
        asset.value - holdings.filter { it.platformId == asset.id }.sumOf { it.cost }
    }

    val history = remember(transactions, asset.id) {
        transactions
            .filter { it.investmentAssetId == asset.id || it.feeAssetId == asset.id }
            .sortedByDescending { it.dateMillis }
            .map { tx ->
                val isFee = tx.feeAssetId == asset.id
                val withdrawSide =
                    if (isFee) tx.note.contains("tarik", ignoreCase = true) else tx.type == "income"
                val kind = when {
                    isFee -> HistoryKind.FEE
                    tx.type == "income" -> HistoryKind.WITHDRAW
                    else -> HistoryKind.TOPUP
                }
                AssetHistory(tx, kind, withdrawSide)
            }
    }

    val totalTopUp = history.filter { it.kind == HistoryKind.TOPUP }.sumOf { it.tx.amount }
    val totalWithdraw = history.filter { it.kind == HistoryKind.WITHDRAW }.sumOf { it.tx.amount }
    val totalFees = history.filter { it.kind == HistoryKind.FEE }.sumOf { it.tx.amount }

    val filtered = when (filter) {
        1 -> history.filter {
            it.kind == HistoryKind.TOPUP ||
                (it.kind == HistoryKind.FEE && !it.withdrawSide)
        }

        2 -> history.filter {
            it.kind == HistoryKind.WITHDRAW ||
                (it.kind == HistoryKind.FEE && it.withdrawSide)
        }

        else -> history
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Ink
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    asset.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Riwayat setor & tarik", color = Muted, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Nilai aset", color = Muted, fontSize = 12.sp)
                Text(
                    rupiah(asset.value),
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Spacer(Modifier.height(2.dp))
                Text("Saldo tersedia ${rupiah(available)}", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryTile("Total setor", totalTopUp, Modifier.weight(1f))
                    SummaryTile("Total tarik", totalWithdraw, Modifier.weight(1f))
                }
                if (totalFees > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Total biaya admin ${rupiah(totalFees)}",
                        color = Muted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        SegmentedChips(
            options = listOf("Semua", "Setor", "Tarik"),
            selectedIndex = filter,
            onSelect = { filter = it }
        )

        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            EmptyHint("Belum ada riwayat setor atau tarik untuk aset ini.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                items(filtered, key = { it.tx.id }) { row ->
                    HistoryRow(row, accounts)
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: Long, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Paper)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, color = Muted, fontSize = 11.sp)
        Text(
            rupiah(value),
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HistoryRow(item: AssetHistory, accounts: List<AccountEntity>) {
    val icon = when (item.kind) {
        HistoryKind.TOPUP -> Icons.Filled.SouthWest
        HistoryKind.WITHDRAW -> Icons.Filled.NorthEast
        HistoryKind.FEE -> Icons.Filled.ReceiptLong
    }
    val tint = when (item.kind) {
        HistoryKind.TOPUP -> Green
        HistoryKind.WITHDRAW -> Red
        HistoryKind.FEE -> Gold
    }
    val soft = when (item.kind) {
        HistoryKind.TOPUP -> GreenSoft
        HistoryKind.WITHDRAW -> RedSoft
        HistoryKind.FEE -> GoldSoft
    }
    val sign = if (item.kind == HistoryKind.TOPUP) "+" else "-"
    val label = when {
        item.kind == HistoryKind.TOPUP &&
            item.tx.note.contains("terjadwal", ignoreCase = true) -> "Setor terjadwal"

        item.kind == HistoryKind.TOPUP -> "Setor"
        item.kind == HistoryKind.WITHDRAW -> "Tarik"
        item.withdrawSide -> "Biaya admin tarik"
        else -> "Biaya admin setor"
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).background(soft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
            Text(
                "${transactionAccountLabel(item.tx, accounts)} · ${formatDate(item.tx.dateMillis)}",
                color = Muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.kind == HistoryKind.FEE && item.tx.note.isNotBlank()) {
                Text(
                    item.tx.note,
                    color = Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            "$sign${rupiah(item.tx.amount)}",
            fontWeight = FontWeight.Bold,
            color = tint,
            fontSize = 14.sp
        )
    }
}

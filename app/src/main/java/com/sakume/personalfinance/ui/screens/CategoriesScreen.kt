package com.sakume.personalfinance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.CATEGORY_KIND_BOTH
import com.sakume.personalfinance.data.CATEGORY_KIND_EXPENSE
import com.sakume.personalfinance.data.CATEGORY_KIND_INCOME
import com.sakume.personalfinance.data.CATEGORY_KIND_TRANSFER
import com.sakume.personalfinance.data.CategoryEntity
import com.sakume.personalfinance.ui.CATEGORY_PALETTE
import com.sakume.personalfinance.ui.PICKABLE_CATEGORY_ICONS
import com.sakume.personalfinance.ui.categoryColor
import com.sakume.personalfinance.ui.categoryIconVector
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Paper
import com.sakume.personalfinance.ui.theme.Red

private fun kindLabel(kind: String) = when (kind) {
    CATEGORY_KIND_EXPENSE -> "Pengeluaran"
    CATEGORY_KIND_INCOME -> "Pemasukan"
    CATEGORY_KIND_BOTH -> "Pemasukan & pengeluaran"
    CATEGORY_KIND_TRANSFER -> "Transfer"
    else -> kind
}

@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    usage: Map<String, Int>,
    onBack: () -> Unit,
    onAdd: (name: String, kind: String, icon: String, color: Long) -> Unit,
    onUpdate: (id: String, name: String, icon: String, color: Long) -> Unit,
    onDelete: (id: String) -> Unit
) {
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Paper).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Ink)
            }
            Text("Kategori", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
            IconButton(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah kategori", tint = Ink)
            }
        }
        Text(
            "Kategori bertanda gembok adalah kategori sistem: dipakai otomatis oleh tagihan, investasi, utang, dan transfer, " +
                "jadi tidak bisa dihapus atau diganti namanya.",
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(categories, key = { it.id }) { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBg)
                        .clickable { editing = c }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(36.dp).background(categoryColor(c).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(categoryIconVector(c.icon), null, tint = categoryColor(c), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "${kindLabel(c.kind)} · ${usage[c.id] ?: 0} transaksi",
                            color = Muted, fontSize = 12.sp
                        )
                    }
                    if (c.isSystem) {
                        Icon(Icons.Filled.Lock, contentDescription = "Kategori sistem", tint = Muted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (adding) {
        CategoryEditSheet(
            existing = null,
            usageCount = 0,
            onDismiss = { adding = false },
            onSave = { name, kind, icon, color -> onAdd(name, kind, icon, color); adding = false },
            onDelete = {}
        )
    }
    editing?.let { c ->
        CategoryEditSheet(
            existing = c,
            usageCount = usage[c.id] ?: 0,
            onDismiss = { editing = null },
            onSave = { name, _, icon, color -> onUpdate(c.id, name, icon, color); editing = null },
            onDelete = { onDelete(c.id); editing = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditSheet(
    existing: CategoryEntity?,
    usageCount: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, kind: String, icon: String, color: Long) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var kind by remember { mutableStateOf(existing?.kind ?: CATEGORY_KIND_EXPENSE) }
    var icon by remember { mutableStateOf(existing?.icon ?: PICKABLE_CATEGORY_ICONS.first()) }
    var color by remember { mutableStateOf(existing?.color ?: CATEGORY_PALETTE.first()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val system = existing?.isSystem == true

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Paper
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (existing == null) "Kategori baru" else "Ubah kategori",
                fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink
            )
            TextField(
                value = name,
                onValueChange = { if (!system) name = it },
                enabled = !system,
                label = { Text("Nama kategori") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
            if (existing == null) {
                Text("Jenis", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(CATEGORY_KIND_EXPENSE, CATEGORY_KIND_INCOME).forEach { k ->
                        Text(
                            kindLabel(k),
                            color = if (kind == k) Color.White else Ink,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (kind == k) Ink else Color.White)
                                .clickable { kind = k }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            if (!system) {
                Text("Ikon", color = Muted, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PICKABLE_CATEGORY_ICONS.forEach { key ->
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (icon == key) Ink else Color.White)
                                .clickable { icon = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(categoryIconVector(key), null, tint = if (icon == key) Color.White else Ink, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Text("Warna", color = Muted, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CATEGORY_PALETTE.forEach { c ->
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(if (color == c) 3.dp else 0.dp, Ink, CircleShape)
                            .clickable { color = c }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            if (confirmDelete) {
                Text(
                    if (usageCount > 0) "$usageCount transaksi akan dipindahkan ke kategori Lainnya. Lanjutkan hapus?"
                    else "Hapus kategori ini?",
                    color = Ink, fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { confirmDelete = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Ink)
                    ) { Text("Batal") }
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White)
                    ) { Text("Hapus") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (existing != null && !system) {
                        Button(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Red)
                        ) { Text("Hapus") }
                    }
                    Button(
                        onClick = { onSave(name, kind, icon, color) },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink)
                    ) { Text("Simpan") }
                }
            }
        }
    }
}

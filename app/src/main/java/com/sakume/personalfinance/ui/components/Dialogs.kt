package com.sakume.personalfinance.ui.components

import android.app.DatePickerDialog
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.ui.AppPrefs
import com.sakume.personalfinance.ui.rupiah
import com.sakume.personalfinance.ui.theme.Accent
import com.sakume.personalfinance.ui.theme.CardBg
import com.sakume.personalfinance.ui.theme.Ink
import com.sakume.personalfinance.ui.theme.Muted
import com.sakume.personalfinance.ui.theme.Paper
import com.sakume.personalfinance.ui.theme.Red
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

private val FieldBg = Color(0xFFFFFFFF)
private val CancelBg = Color(0xFFE8E8E8)

fun formatRibuan(digits: String): String {
    if (digits.isEmpty()) return ""
    val n = digits.toLongOrNull() ?: return digits
    return NumberFormat.getInstance(Locale("id", "ID")).format(n)
}

fun digitsOnly(s: String): String = s.filter { it.isDigit() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBottomSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Paper
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            content()
        }
    }
}

private class AutoTrailingZerosTransformation(private val zeroCount: Int) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }
        if (digits.isEmpty() || zeroCount <= 0) {
            val shown = if (digits.isEmpty()) "" else formatRibuan(digits)
            val mapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = shown.length
                override fun transformedToOriginal(offset: Int): Int = digits.length
            }
            return TransformedText(AnnotatedString(shown), mapping)
        }
        val full = digits + "0".repeat(zeroCount)
        val formatted = formatRibuan(full)
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = formatted.length
            override fun transformedToOriginal(offset: Int): Int = digits.length
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
private fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isMoney: Boolean = false,
    autoThousand: Boolean = false,
    singleLine: Boolean = true
) {
    if (isMoney) {
        val context = LocalContext.current
        val zeroCount = remember { AppPrefs.trailingZeros(context) }
        val zerosStr = if (zeroCount > 0) "0".repeat(zeroCount) else ""
        val digits = digitsOnly(value)
        val displayText = if (autoThousand) digits else formatRibuan(digits)
        val tf = TextFieldValue(displayText, TextRange(displayText.length))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = tf,
                onValueChange = { incoming ->
                    val dig = digitsOnly(incoming.text)
                    onValueChange(dig)
                },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = if (autoThousand && zeroCount > 0) {
                    AutoTrailingZerosTransformation(zeroCount)
                } else if (autoThousand) {
                    VisualTransformation { text ->
                        val d = text.text.filter { it.isDigit() }
                        val shown = if (d.isEmpty()) "" else formatRibuan(d)
                        val mapping = object : OffsetMapping {
                            override fun originalToTransformed(offset: Int) = shown.length
                            override fun transformedToOriginal(offset: Int) = d.length
                        }
                        TransformedText(AnnotatedString(shown), mapping)
                    }
                } else {
                    VisualTransformation.None
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FieldBg,
                    unfocusedContainerColor = FieldBg,
                    disabledContainerColor = FieldBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }
    } else {
        TextField(
            value = value,
            onValueChange = { onValueChange(it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }.let { s ->
                buildString {
                    var cap = true
                    s.forEach { ch ->
                        when {
                            ch.isWhitespace() -> { append(ch); cap = true }
                            cap && ch.isLetter() -> { append(ch.uppercaseChar()); cap = false }
                            else -> append(ch)
                        }
                    }
                }
            }) },
            label = { Text(label) },
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg,
                disabledContainerColor = FieldBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SheetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    destructive: Boolean = false,
    enabled: Boolean = true
) {
    val bg = when {
        destructive -> Red
        primary -> Ink
        else -> CancelBg
    }
    val fg = when {
        primary || destructive -> Color.White
        else -> Ink
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = fg,
            disabledContainerColor = CancelBg,
            disabledContentColor = Muted
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FormDialog(
    title: String,
    fields: List<String>,
    options: List<String>,
    optionLabel: String = "Pilihan",
    optionDisplay: (String) -> String = { it },
    optionBeforeField: Int = -1,
    extraOptions: List<String> = emptyList(),
    extraOptionLabel: String = "Pilihan tambahan",
    tertiaryOptions: List<String> = emptyList(),
    tertiaryOptionLabel: String = "Pilihan lain",
    tertiaryDisplay: (String) -> String = { it },
    optionSecondary: (String) -> String? = { null },
    extraOptionSecondary: (String) -> String? = { null },
    tertiaryOptionSecondary: (String) -> String? = { null },
    initialOptionIndex: Int = 0,
    initialExtraIndex: Int = 0,
    initialTertiaryIndex: Int = 0,
    moneyFields: Set<Int> = emptySet(),
    manualMoneyFields: Set<Int> = emptySet(),
    fieldSuggestions: Map<Int, List<String>> = emptyMap(),
    requiredFields: Set<Int> = emptySet(),
    includeDueDate: Boolean = false,
    dueDateLabel: String = "Tanggal jatuh tempo",
    dueDateOptional: Boolean = false,
    initialDueDateMillis: Long? = null,
    initialValues: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val values = remember(title) {
        mutableStateListOf<String>().apply {
            fields.indices.forEach { i -> add(initialValues.getOrElse(i) { "" }) }
        }
    }
    var selectedOpt by remember(title) { mutableIntStateOf(initialOptionIndex.coerceIn(0, maxOf(options.lastIndex, 0))) }
    var selectedExtra by remember(title) { mutableIntStateOf(initialExtraIndex.coerceIn(0, maxOf(extraOptions.lastIndex, 0))) }
    var selectedTertiary by remember(title) {
        val def = if (tertiaryOptions.isEmpty()) 0 else initialTertiaryIndex.coerceIn(0, tertiaryOptions.lastIndex)
        mutableIntStateOf(def)
    }
    var expandedSuggestion by remember { mutableStateOf(-1) }
    var dueDateMillis by remember(title) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        mutableStateOf<Long?>(initialDueDateMillis ?: if (dueDateOptional) null else cal.timeInMillis)
    }
    val context = LocalContext.current

    fun formatDue(ms: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        val months = arrayOf("Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")
        return "%02d %s %d".format(c.get(Calendar.DAY_OF_MONTH), months[c.get(Calendar.MONTH)], c.get(Calendar.YEAR))
    }

    val optionsBlock: @Composable () -> Unit = {
        if (options.isNotEmpty()) {
            if (optionLabel.isNotBlank()) {
                Text(optionLabel, color = Muted, fontSize = 12.sp)
            }
            val isTenor = optionLabel.contains("Tenor", ignoreCase = true)
            val useChips = isTenor || (
                options.size <= 3 &&
                    !optionLabel.contains("rekening", ignoreCase = true) &&
                    !optionLabel.contains("Dari", ignoreCase = true) &&
                    !optionLabel.contains("Ke ", ignoreCase = true)
                )
            if (useChips) {
                SegmentedChips(
                    options = options.map(optionDisplay),
                    selectedIndex = selectedOpt,
                    onSelect = { selectedOpt = it }
                )
            } else {
                OptionCardList(options, selectedOpt, optionDisplay, optionSecondary) { selectedOpt = it }
            }
        }
    }

    AppBottomSheet(onDismiss) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            fields.forEachIndexed { i, field ->
                if (optionBeforeField == i) optionsBlock()
                val suggestions = fieldSuggestions[i].orEmpty()
                if (suggestions.isNotEmpty() && !(i in moneyFields || field.contains("Nominal", true) || field.contains("Saldo", true) || field.contains("Target", true))) {
                    Box {
                        SheetField(
                            value = values[i],
                            onValueChange = { values[i] = it },
                            label = field,
                            isMoney = false
                        )
                    }
                    val shown = suggestions.take(12)
                    SegmentedChips(
                        options = shown,
                        selectedIndex = shown.indexOfFirst { it.equals(values[i], ignoreCase = true) },
                        onSelect = { values[i] = shown[it] }
                    )
                } else {
                    val money = i in moneyFields || field.contains("Nominal", true) || field.contains("Saldo", true) || field.contains("Target", true)
                    val autoZeros = money &&
                        field.contains("Nominal", ignoreCase = true) &&
                        !field.contains("Total", ignoreCase = true)
                    SheetField(
                        value = values[i],
                        onValueChange = { values[i] = it },
                        label = field,
                        isMoney = money,
                        autoThousand = autoZeros
                    )
                }
                if (includeDueDate && i == minOf(1, fields.lastIndex)) {
                    Text(dueDateLabel, color = Muted, fontSize = 12.sp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FieldBg)
                            .clickable {
                                val c = Calendar.getInstance().apply { timeInMillis = dueDateMillis ?: System.currentTimeMillis() }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        dueDateMillis = Calendar.getInstance().apply {
                                            set(y, m, d, 0, 0, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    },
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val due = dueDateMillis
                        Column {
                            Text(
                                if (due != null) formatDue(due) else "Tidak diatur",
                                color = if (due != null) Ink else Muted,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                if (dueDateOptional) "Ketuk untuk pilih tanggal (opsional)" else "Ketuk untuk pilih tanggal",
                                color = Muted,
                                fontSize = 11.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (dueDateOptional && due != null) {
                                Text(
                                    "Hapus",
                                    color = Muted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { dueDateMillis = null }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text("📅", fontSize = 18.sp)
                        }
                    }
                }
            }
            if (optionBeforeField !in fields.indices) optionsBlock()
            if (extraOptions.isNotEmpty()) {
                Text(extraOptionLabel, color = Muted, fontSize = 12.sp)
                OptionCardList(extraOptions, selectedExtra, secondary = extraOptionSecondary) { selectedExtra = it }
            }
            if (tertiaryOptions.isNotEmpty()) {
                Text(tertiaryOptionLabel, color = Muted, fontSize = 12.sp)
                OptionCardList(tertiaryOptions, selectedTertiary, tertiaryDisplay, tertiaryOptionSecondary) { selectedTertiary = it }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetButton("Batal", onDismiss, Modifier.weight(1f), primary = false)
            SheetButton("Simpan", {
                val result = values.toMutableList()
                if (includeDueDate) result.add(dueDateMillis?.toString().orEmpty())
                if (options.isNotEmpty()) result.add(options.getOrElse(selectedOpt) { options.first() })
                if (extraOptions.isNotEmpty()) result.add(extraOptions.getOrElse(selectedExtra) { extraOptions.first() })
                if (tertiaryOptions.isNotEmpty()) result.add(tertiaryOptions.getOrElse(selectedTertiary) { tertiaryOptions.first() })
                onSave(result)
            }, Modifier.weight(1f), primary = true,
                enabled = requiredFields.all { values[it].isNotBlank() })
        }
    }
}

@Composable
fun CashTransferDialog(
    accounts: List<AccountEntity>,
    isCash: (AccountEntity) -> Boolean,
    initialWithdraw: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (isWithdraw: Boolean, otherAccount: AccountEntity, cashAccount: AccountEntity, amount: Long, note: String) -> Unit
) {
    val context = LocalContext.current
    val cashAccounts = accounts.filter(isCash)
    val otherAccounts = accounts.filterNot(isCash)

    var isWithdraw by remember { mutableStateOf(initialWithdraw) }
    var otherSelected by remember { mutableStateOf(otherAccounts.firstOrNull()?.id ?: "") }
    var cashSelected by remember { mutableStateOf(cashAccounts.firstOrNull()?.id ?: "") }
    var amountDigits by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val otherAccount = otherAccounts.firstOrNull { it.id == otherSelected } ?: otherAccounts.firstOrNull()
    val cashAccount = cashAccounts.firstOrNull { it.id == cashSelected } ?: cashAccounts.firstOrNull()

    AppBottomSheet(onDismiss) {
        Text(if (isWithdraw) "Tarik tunai" else "Setor tunai", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegmentedChips(
                options = listOf("Tarik tunai", "Setor tunai"),
                selectedIndex = if (isWithdraw) 0 else 1,
                onSelect = { isWithdraw = it == 0 }
            )
        }
        Spacer(Modifier.height(14.dp))

        if (otherAccounts.isEmpty() || cashAccounts.isEmpty()) {
            Text(
                "Butuh minimal satu rekening tunai dan satu rekening non-tunai untuk ${if (isWithdraw) "tarik" else "setor"} tunai.",
                color = Muted,
                fontSize = 13.sp
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SheetField(
                    value = amountDigits,
                    onValueChange = { amountDigits = it },
                    label = "Nominal",
                    isMoney = true,
                    autoThousand = true
                )
                SheetField(value = note, onValueChange = { note = it }, label = "Catatan (opsional)")

                val cashName = { a: AccountEntity -> if (isCash(a)) "Cash" else a.name }

                Text(if (isWithdraw) "Dari rekening" else "Dari Cash", color = Muted, fontSize = 12.sp)
                if (isWithdraw) {
                    AccountCardList(otherAccounts, otherAccount?.id ?: "", cashName) { otherSelected = it }
                } else {
                    AccountCardList(cashAccounts, cashAccount?.id ?: "", cashName) { cashSelected = it }
                }

                Text(if (isWithdraw) "Ke Cash" else "Ke rekening", color = Muted, fontSize = 12.sp)
                if (isWithdraw) {
                    AccountCardList(cashAccounts, cashAccount?.id ?: "", cashName) { cashSelected = it }
                } else {
                    AccountCardList(otherAccounts, otherAccount?.id ?: "", cashName) { otherSelected = it }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetButton("Batal", onDismiss, Modifier.weight(1f), primary = false)
            SheetButton(
                if (isWithdraw) "Tarik" else "Setor",
                {
                    val mul = AppPrefs.multiplier(AppPrefs.trailingZeros(context))
                    val amount = (amountDigits.toLongOrNull() ?: 0L) * mul
                    val other = otherAccount
                    val cash = cashAccount
                    if (other != null && cash != null && amount > 0) {
                        onConfirm(isWithdraw, other, cash, amount, note)
                    }
                },
                Modifier.weight(1f),
                primary = true,
                enabled = otherAccount != null && cashAccount != null && (amountDigits.toLongOrNull() ?: 0L) > 0
            )
        }
    }
}

@Composable
private fun OptionCard(
    primary: String,
    secondary: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBg)
            .then(if (selected) Modifier.border(1.5.dp, Ink, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            primary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = Ink,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (secondary != null) {
            Spacer(Modifier.width(8.dp))
            Text(secondary, color = Muted, fontSize = 12.sp, maxLines = 1)
        }
    }
}


@Composable
private fun CardPickerField(
    count: Int,
    selectedIndex: Int,
    primary: (Int) -> String,
    secondary: (Int) -> String? = { null },
    onSelect: (Int) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    var fieldWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val hasSelection = selectedIndex in 0 until count
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(FieldBg)
                .onSizeChanged { fieldWidthPx = it.width }
                .clickable(enabled = count > 0) { open = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (hasSelection) primary(selectedIndex) else if (count == 0) "Belum ada pilihan" else "Pilih",
                color = if (hasSelection) Ink else Muted,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (hasSelection) {
                secondary(selectedIndex)?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(it, color = Muted, fontSize = 12.sp, maxLines = 1)
                }
            }
            
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier
                .width(with(density) { fieldWidthPx.toDp() })
                .heightIn(max = 280.dp),
            offset = DpOffset(0.dp, 4.dp),
            shape = RoundedCornerShape(14.dp),
            containerColor = Paper,
            tonalElevation = 0.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0 until count) {
                    OptionCard(
                        primary = primary(i),
                        secondary = secondary(i),
                        selected = selectedIndex == i,
                        onClick = { onSelect(i) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionCardList(
    options: List<String>,
    selectedIndex: Int,
    display: (String) -> String = { it },
    secondary: (String) -> String? = { null },
    onSelect: (Int) -> Unit
) {
    CardPickerField(
        count = options.size,
        selectedIndex = selectedIndex,
        primary = { display(options[it]) },
        secondary = { secondary(options[it]) },
        onSelect = onSelect
    )
}

@Composable
private fun AccountCardList(
    accounts: List<AccountEntity>,
    selectedId: String,
    nameOf: (AccountEntity) -> String = { it.name },
    onSelect: (String) -> Unit
) {
    CardPickerField(
        count = accounts.size,
        selectedIndex = accounts.indexOfFirst { it.id == selectedId },
        primary = { nameOf(accounts[it]) },
        secondary = { rupiah(accounts[it].balance) },
        onSelect = { onSelect(accounts[it].id) }
    )
}

private val scheduleDays = listOf(
    2 to "Senin", 3 to "Selasa", 4 to "Rabu", 5 to "Kamis", 6 to "Jumat", 7 to "Sabtu", 1 to "Minggu"
)

@Composable
fun WeeklyScheduleDialog(
    title: String,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (account: AccountEntity, amount: Long, dayOfWeek: Int) -> Unit
) {
    val context = LocalContext.current
    var amountDigits by remember { mutableStateOf("") }
    var day by remember { mutableIntStateOf(2) }
    var selected by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }

    AppBottomSheet(onDismiss) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetField(
                value = amountDigits,
                onValueChange = { amountDigits = it },
                label = "Nominal per minggu",
                isMoney = true,
                autoThousand = true
            )
            Text("Hari (Senin–Minggu)", color = Muted, fontSize = 12.sp)
            SegmentedChips(
                options = scheduleDays.map { it.second },
                selectedIndex = scheduleDays.indexOfFirst { it.first == day },
                onSelect = { day = scheduleDays[it].first }
            )
            Text("Dari rekening", color = Muted, fontSize = 12.sp)
            AccountCardList(accounts, selected) { selected = it }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetButton("Batal", onDismiss, Modifier.weight(1f), primary = false)
            SheetButton(
                "Simpan",
                {
                    val account = accounts.firstOrNull { it.id == selected }
                    if (account != null) {
                        val amount = (amountDigits.toLongOrNull() ?: 0L) *
                            AppPrefs.multiplier(AppPrefs.trailingZeros(context))
                        onConfirm(account, amount, day)
                    }
                },
                Modifier.weight(1f),
                primary = true,
                enabled = accounts.isNotEmpty()
            )
        }
    }
}

@Composable
fun InvestmentTransferDialog(
    title: String,
    accountLabel: String,
    accounts: List<AccountEntity>,
    infoText: String? = null,
    amountLabel: String = "Nominal",
    feeLabel: String = "Biaya Admin",
    onDismiss: () -> Unit,
    onConfirm: (account: AccountEntity, amount: Long, adminFee: Long) -> Unit
) {
    val context = LocalContext.current
    var amountDigits by remember { mutableStateOf("") }
    var feeDigits by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }

    AppBottomSheet(onDismiss) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
        if (infoText != null) {
            Spacer(Modifier.height(4.dp))
            Text(infoText, color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetField(
                value = amountDigits,
                onValueChange = { amountDigits = it },
                label = amountLabel,
                isMoney = true,
                autoThousand = true
            )
            SheetField(
                value = feeDigits,
                onValueChange = { feeDigits = it },
                label = feeLabel,
                isMoney = true,
                autoThousand = false
            )
            Text(accountLabel, color = Muted, fontSize = 12.sp)
            AccountCardList(accounts, selected) { selected = it }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetButton("Batal", onDismiss, Modifier.weight(1f), primary = false)
            SheetButton(
                "Simpan",
                {
                    val account = accounts.firstOrNull { it.id == selected }
                    if (account != null) {
                        val amount = (amountDigits.toLongOrNull() ?: 0L) *
                            AppPrefs.multiplier(AppPrefs.trailingZeros(context))
                        val fee = feeDigits.toLongOrNull() ?: 0L
                        onConfirm(account, amount, fee)
                    }
                },
                Modifier.weight(1f),
                primary = true,
                enabled = accounts.isNotEmpty()
            )
        }
    }
}

@Composable
fun AccountPickerDialog(
    title: String,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSelect: (AccountEntity) -> Unit
) {
    var selected by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    AppBottomSheet(onDismiss) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
        Spacer(Modifier.height(10.dp))
        if (accounts.isEmpty()) {
            Text("Belum ada rekening.", color = Muted)
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(accounts, key = { it.id }) { a ->
                    OptionCard(
                        primary = a.name,
                        secondary = rupiah(a.balance),
                        selected = selected == a.id,
                        onClick = { selected = a.id }
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetButton("Batal", onDismiss, Modifier.weight(1f), primary = false)
            SheetButton(
                "Pilih",
                { accounts.firstOrNull { it.id == selected }?.let(onSelect) },
                Modifier.weight(1f),
                primary = true,
                enabled = accounts.isNotEmpty()
            )
        }
    }
}

@Composable
fun DebtPaymentDialog(
    title: String,
    actionLabel: String,
    total: Long,
    paid: Long,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (account: AccountEntity, amount: Long) -> Unit
) {
    val context = LocalContext.current
    val mul = remember { AppPrefs.multiplier(AppPrefs.trailingZeros(context)) }.coerceAtLeast(1L)
    val remaining = (total - paid).coerceAtLeast(0L)
    var amountDigits by remember { mutableStateOf(if (remaining % mul == 0L) (remaining / mul).toString() else "") }
    var exactAmount by remember { mutableStateOf<Long?>(if (remaining % mul == 0L) null else remaining) }
    var selected by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }

    fun setAmount(value: Long) {
        if (value % mul == 0L) {
            amountDigits = (value / mul).toString()
            exactAmount = null
        } else {
            amountDigits = ""
            exactAmount = value
        }
    }

    val amount = exactAmount ?: ((amountDigits.toLongOrNull() ?: 0L) * mul)
    val valid = amount in 1L..remaining && accounts.isNotEmpty()

    AppBottomSheet(onDismiss) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "Total ${rupiah(total)}, Terbayar ${rupiah(paid)}, Kurang ${rupiah(remaining)}",
            color = Muted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(7.dp))
        SheetField(
            value = amountDigits,
            onValueChange = {
                amountDigits = it.trimStart('0')
                exactAmount = null
            },
            label = "Nominal $actionLabel",
            isMoney = true,
            autoThousand = true
        )
        if (amount > remaining) {
            Spacer(Modifier.height(6.dp))
            Text("Nominal melebihi sisa (${rupiah(remaining)})", color = Red, fontSize = 12.sp)
        } else if (amount in 1L until remaining) {
            Spacer(Modifier.height(6.dp))
            Text("Sisa setelah ini ${rupiah(remaining - amount)}", color = Ink, fontSize = 12.sp)
        } else if (amount == remaining && remaining > 0L) {
            Spacer(Modifier.height(6.dp))
            Text("Akan ditandai lunas", color = Ink, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text("Rekening", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        AccountCardList(accounts, selected) { selected = it }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetButton("Batal", onDismiss, Modifier.weight(1f), primary = false)
            SheetButton(
                if (amount == remaining) "Lunasi" else "Simpan",
                { accounts.firstOrNull { it.id == selected }?.let { onConfirm(it, amount) } },
                Modifier.weight(1f),
                primary = true,
                enabled = valid
            )
        }
    }
}

@Composable
fun ConfirmDialog(title: String, confirm: () -> Unit, cancel: () -> Unit) {
    AppBottomSheet(cancel) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetButton("Batal", cancel, Modifier.weight(1f), primary = false)
            SheetButton("Ya, lanjutkan", confirm, Modifier.weight(1f), primary = true, destructive = true)
        }
    }
}

@Composable
fun MessageDialog(title: String, message: String, onDismiss: () -> Unit) {
    AppBottomSheet(onDismiss) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        SheetButton("OK", onDismiss, Modifier.fillMaxWidth(), primary = true)
    }
}

@Composable
fun RestoreBackupDialog(
    title: String,
    detail: String,
    canRestore: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text(detail, color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetButton("Batal", onDismiss, Modifier.weight(1f), primary = false)
            SheetButton("Pulihkan", onRestore, Modifier.weight(1f), primary = true, destructive = true, enabled = canRestore)
        }
        Spacer(Modifier.height(8.dp))
        SheetButton("Hapus file backup ini", onDelete, Modifier.fillMaxWidth(), primary = false)
    }
}

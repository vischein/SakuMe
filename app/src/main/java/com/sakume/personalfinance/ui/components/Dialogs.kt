package com.sakume.personalfinance.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
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
            if (zeroCount > 0) {
                Text(
                    text = zerosStr,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Ink)
                        .clickable {
                            val dig = digits + zerosStr
                            onValueChange(dig)
                        }
                        .padding(horizontal = 14.dp, vertical = 16.dp)
                )
            }
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
    extraOptions: List<String> = emptyList(),
    extraOptionLabel: String = "Pilihan tambahan",
    tertiaryOptions: List<String> = emptyList(),
    tertiaryOptionLabel: String = "Pilihan lain",
    tertiaryDisplay: (String) -> String = { it },
    initialOptionIndex: Int = 0,
    initialExtraIndex: Int = 0,
    initialTertiaryIndex: Int = 0,
    moneyFields: Set<Int> = emptySet(),
    fieldSuggestions: Map<Int, List<String>> = emptyMap(),
    includeDueDate: Boolean = false,
    dueDateLabel: String = "Tanggal jatuh tempo",
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
    var expanded by remember { mutableStateOf(false) }
    var expandedExtra by remember { mutableStateOf(false) }
    var expandedTertiary by remember { mutableStateOf(false) }
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
        mutableStateOf(cal.timeInMillis)
    }
    val context = LocalContext.current

    fun formatDue(ms: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        val months = arrayOf("Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")
        return "%02d %s %d".format(c.get(Calendar.DAY_OF_MONTH), months[c.get(Calendar.MONTH)], c.get(Calendar.YEAR))
    }

    AppBottomSheet(onDismiss) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            fields.forEachIndexed { i, field ->
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
                    if (suggestions.isNotEmpty()) {
                        Text("Pilih yang sudah ada", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(suggestions.take(12).size) { idx ->
                                val s = suggestions[idx]
                                val selected = values[i].equals(s, ignoreCase = true)
                                Text(
                                    text = s,
                                    color = if (selected) Color.White else Ink,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) Ink else FieldBg)
                                        .clickable { values[i] = s }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
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
                                val c = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
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
                        Column {
                            Text(formatDue(dueDateMillis), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Ketuk untuk pilih tanggal", color = Muted, fontSize = 11.sp)
                        }
                        Text("📅", fontSize = 18.sp)
                    }
                }
            }
            if (options.isNotEmpty()) {
                Text(optionLabel, color = Muted, fontSize = 12.sp)
                val isTenor = optionLabel.contains("Tenor", ignoreCase = true)
                val useChips = isTenor || (
                    options.size <= 3 &&
                        !optionLabel.contains("rekening", ignoreCase = true) &&
                        !optionLabel.contains("Dari", ignoreCase = true) &&
                        !optionLabel.contains("Ke ", ignoreCase = true)
                    )
                if (useChips) {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(options.size) { i ->
                            val opt = options[i]
                            SelectableChip(
                                label = optionDisplay(opt),
                                selected = selectedOpt == i,
                                onClick = { selectedOpt = i }
                            )
                        }
                    }
                } else {
                    Box {
                        Text(
                            text = optionDisplay(options.getOrElse(selectedOpt) { options.first() }),
                            color = Ink,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(FieldBg)
                                .clickable { expanded = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = CardBg) {
                            options.forEachIndexed { i, opt ->
                                DropdownMenuItem(
                                    text = { Text(optionDisplay(opt), color = Ink) },
                                    onClick = { selectedOpt = i; expanded = false },
                                    colors = MenuDefaults.itemColors(textColor = Ink)
                                )
                            }
                        }
                    }
                }
            }
            if (extraOptions.isNotEmpty()) {
                Text(extraOptionLabel, color = Muted, fontSize = 12.sp)
                Box {
                    Text(
                        text = extraOptions.getOrElse(selectedExtra) { extraOptions.firstOrNull().orEmpty() },
                        color = Ink,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FieldBg)
                            .clickable { expandedExtra = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                    DropdownMenu(expanded = expandedExtra, onDismissRequest = { expandedExtra = false }, containerColor = CardBg) {
                        extraOptions.forEachIndexed { i, opt ->
                            DropdownMenuItem(
                                text = { Text(opt, color = Ink) },
                                onClick = { selectedExtra = i; expandedExtra = false },
                                colors = MenuDefaults.itemColors(textColor = Ink)
                            )
                        }
                    }
                }
            }
            if (tertiaryOptions.isNotEmpty()) {
                Text(tertiaryOptionLabel, color = Muted, fontSize = 12.sp)
                Box {
                    Text(
                        text = tertiaryDisplay(tertiaryOptions.getOrElse(selectedTertiary) { tertiaryOptions.first() }),
                        color = Ink,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FieldBg)
                            .clickable { expandedTertiary = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                    DropdownMenu(expanded = expandedTertiary, onDismissRequest = { expandedTertiary = false }, containerColor = CardBg) {
                        tertiaryOptions.forEachIndexed { i, opt ->
                            DropdownMenuItem(
                                text = { Text(tertiaryDisplay(opt), color = Ink) },
                                onClick = { selectedTertiary = i; expandedTertiary = false },
                                colors = MenuDefaults.itemColors(textColor = Ink)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetButton("Batal", onDismiss, Modifier.weight(1f), primary = false)
            SheetButton("Simpan", {
                val result = values.toMutableList()
                if (includeDueDate) result.add(dueDateMillis.toString())
                if (options.isNotEmpty()) result.add(options.getOrElse(selectedOpt) { options.first() })
                if (extraOptions.isNotEmpty()) result.add(extraOptions.getOrElse(selectedExtra) { extraOptions.first() })
                if (tertiaryOptions.isNotEmpty()) result.add(tertiaryOptions.getOrElse(selectedTertiary) { tertiaryOptions.first() })
                onSave(result)
            }, Modifier.weight(1f), primary = true)
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
    var expandedOther by remember { mutableStateOf(false) }
    var expandedCash by remember { mutableStateOf(false) }
    var amountDigits by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val otherAccount = otherAccounts.firstOrNull { it.id == otherSelected } ?: otherAccounts.firstOrNull()
    val cashAccount = cashAccounts.firstOrNull { it.id == cashSelected } ?: cashAccounts.firstOrNull()

    AppBottomSheet(onDismiss) {
        Text(if (isWithdraw) "Tarik tunai" else "Setor tunai", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableChip(label = "Tarik tunai", selected = isWithdraw) { isWithdraw = true }
            SelectableChip(label = "Setor tunai", selected = !isWithdraw) { isWithdraw = false }
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

                Text(if (isWithdraw) "Dari rekening" else "Dari Cash", color = Muted, fontSize = 12.sp)
                Box {
                    val fromLabel = if (isWithdraw) (otherAccount?.name ?: "Pilih rekening") else (cashAccount?.let { if (isCash(it)) "Cash" else it.name } ?: "Pilih Cash")
                    Text(
                        text = fromLabel,
                        color = Ink,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FieldBg)
                            .clickable { if (isWithdraw) expandedOther = true else expandedCash = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                    DropdownMenu(expanded = if (isWithdraw) expandedOther else expandedCash, onDismissRequest = { expandedOther = false; expandedCash = false }, containerColor = CardBg) {
                        val list = if (isWithdraw) otherAccounts else cashAccounts
                        list.forEach { a ->
                            DropdownMenuItem(
                                text = { Text("${if (isCash(a)) "Cash" else a.name} · ${rupiah(a.balance)}", color = Ink) },
                                onClick = {
                                    if (isWithdraw) { otherSelected = a.id; expandedOther = false }
                                    else { cashSelected = a.id; expandedCash = false }
                                },
                                colors = MenuDefaults.itemColors(textColor = Ink)
                            )
                        }
                    }
                }

                Text(if (isWithdraw) "Ke Cash" else "Ke rekening", color = Muted, fontSize = 12.sp)
                Box {
                    val toLabel = if (isWithdraw) (cashAccount?.let { if (isCash(it)) "Cash" else it.name } ?: "Pilih Cash") else (otherAccount?.name ?: "Pilih rekening")
                    Text(
                        text = toLabel,
                        color = Ink,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FieldBg)
                            .clickable { if (isWithdraw) expandedCash = true else expandedOther = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                    DropdownMenu(expanded = if (isWithdraw) expandedCash else expandedOther, onDismissRequest = { expandedOther = false; expandedCash = false }, containerColor = CardBg) {
                        val list = if (isWithdraw) cashAccounts else otherAccounts
                        list.forEach { a ->
                            DropdownMenuItem(
                                text = { Text("${if (isCash(a)) "Cash" else a.name} · ${rupiah(a.balance)}", color = Ink) },
                                onClick = {
                                    if (isWithdraw) { cashSelected = a.id; expandedCash = false }
                                    else { otherSelected = a.id; expandedOther = false }
                                },
                                colors = MenuDefaults.itemColors(textColor = Ink)
                            )
                        }
                    }
                }

                SheetField(value = note, onValueChange = { note = it }, label = "Catatan (opsional)")
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
fun AccountPickerDialog(
    title: String,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSelect: (AccountEntity) -> Unit
) {
    var selected by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    AppBottomSheet(onDismiss) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
        Spacer(Modifier.height(12.dp))
        if (accounts.isEmpty()) {
            Text("Belum ada rekening.", color = Muted)
        } else {
            Column {
                accounts.forEach { a ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected == a.id) Paper else Color.Transparent)
                            .clickable { selected = a.id }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == a.id, onClick = { selected = a.id })
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(a.name, fontWeight = FontWeight.Medium)
                            Text(rupiah(a.balance), color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
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

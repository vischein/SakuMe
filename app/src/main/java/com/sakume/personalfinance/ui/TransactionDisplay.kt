package com.sakume.personalfinance.ui

import com.sakume.personalfinance.data.AccountEntity
import com.sakume.personalfinance.data.TransactionEntity

fun buildDisplayTransactions(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>
): List<TransactionEntity> {
    val result = mutableListOf<TransactionEntity>()
    val handledGroups = mutableSetOf<String>()

    transactions
        .sortedByDescending { it.dateMillis }
        .forEach { item ->
            val group = item.transferGroupId

            if (group == null) {
                result += item
                return@forEach
            }

            if (!handledGroups.add(group)) return@forEach

            val pair = transactions.filter { it.transferGroupId == group }
            val out = pair.firstOrNull { it.type == "transfer_out" } ?: item
            val input = pair.firstOrNull { it.type == "transfer_in" }

            val fromName = accounts
                .firstOrNull { it.id == out.accountId }
                ?.name
                ?: "Rekening"

            val toName = accounts
                .firstOrNull { it.id == input?.accountId }
                ?.name
                ?: "Rekening"

            val isCashOut =
                out.note.contains("tarik tunai", ignoreCase = true) ||
                    toName.equals("Cash", ignoreCase = true) ||
                    toName.contains("Dompet Tunai", ignoreCase = true)

            val isCashIn =
                out.note.contains("setor tunai", ignoreCase = true) ||
                    fromName.equals("Cash", ignoreCase = true) ||
                    fromName.contains("Dompet Tunai", ignoreCase = true)

            val display = when {
                isCashOut -> {
                    out.copy(
                        type = "expense",
                        category = "Tarik tunai",
                        note = "Tarik tunai"
                    )
                }

                isCashIn -> {
                    (input ?: item).copy(
                        type = "income",
                        category = "Setor tunai",
                        note = "Setor tunai"
                    )
                }

                else -> {
                    out.copy(
                        category = "$fromName → $toName",
                        note = "Transfer"
                    )
                }
            }

            result += display
        }

    return result
}

fun transactionAccountLabel(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>
): String {
    if (transaction.note == "Transfer") {
        return transaction.category
    }

    return accounts
        .firstOrNull { it.id == transaction.accountId }
        ?.let { account ->
            if (
                account.name.contains("tunai", ignoreCase = true) ||
                account.name.equals("Cash", ignoreCase = true)
            ) {
                "Cash"
            } else {
                account.name
            }
        }
        ?: "Rekening"
}

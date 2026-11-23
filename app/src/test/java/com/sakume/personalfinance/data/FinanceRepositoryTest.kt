package com.sakume.personalfinance.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FinanceRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: FinanceRepository
    private lateinit var cashId: String

    @Before fun setUp() {
        kotlinx.coroutines.runBlocking {
            db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java
            ).allowMainThreadQueries().build()
            repo = FinanceRepository(db)
            repo.addAccount("Cash", "Cash", 100_000L)
            cashId = db.dao().allAccounts().single().id
        }
    }

    @After fun tearDown() { if (::db.isInitialized) db.close() }

    @Test fun addExpenseReducesBalanceWhenFundsAreSufficient() = runTest {
        repo.addTransaction("expense", 25_000, "Makan", cashId, 1_700_000_000_000, "Lunch")
        assertEquals(75_000L, db.dao().account(cashId)!!.balance)
    }

    @Test fun addExpenseRejectsInsufficientBalanceWithoutChangingData() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repo.addTransaction("expense", 100_001, "Makan", cashId, 1_700_000_000_000, "") }
        }
        assertEquals(100_000L, db.dao().account(cashId)!!.balance)
        assertTrue(db.dao().allTransactions().isEmpty())
    }

    @Test fun addIncomeIncreasesBalance() = runTest {
        repo.addTransaction("income", 12_000, "Gaji", cashId, 1_700_000_000_000, "")
        assertEquals(112_000L, db.dao().account(cashId)!!.balance)
    }

    @Test fun transferMovesFundsAndCreatesPairedTransactions() = runTest {
        repo.addAccount("Bank", "Bank", 5_000)
        val bank = db.dao().allAccounts().first { it.name == "Bank" }
        repo.transfer(cashId, bank.id, 30_000)
        assertEquals(70_000L, db.dao().account(cashId)!!.balance)
        assertEquals(35_000L, db.dao().account(bank.id)!!.balance)
        val txs = db.dao().allTransactions()
        assertEquals(2, txs.size)
        assertEquals(txs[0].transferGroupId, txs[1].transferGroupId)
    }

    @Test fun transferRejectsInsufficientFundsAtomically() = runTest {
        repo.addAccount("Bank", "Bank", 0)
        val bank = db.dao().allAccounts().first { it.name == "Bank" }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repo.transfer(cashId, bank.id, 100_001) } }
        assertEquals(100_000L, db.dao().account(cashId)!!.balance)
        assertTrue(db.dao().allTransactions().isEmpty())
    }


    @Test fun transferFromBankToCashActsAsCashWithdrawal() = runTest {
        repo.addAccount("Bank", "Bank", 50_000)
        val bank = db.dao().allAccounts().first { it.name == "Bank" }
        repo.transfer(bank.id, cashId, 20_000)
        assertEquals(30_000L, db.dao().account(bank.id)!!.balance)
        assertEquals(120_000L, db.dao().account(cashId)!!.balance)
    }

    @Test fun payBillAndMarkBillUnpaidReverseBalanceAndLinkedTransaction() = runTest {
        val bill = BillEntity("bill-1", "PLN", "Listrik", 20_000, 1_700_000_000_000)
        db.dao().putBill(bill)
        repo.payBill(bill.id, cashId)
        assertEquals(80_000L, db.dao().account(cashId)!!.balance)
        assertTrue(db.dao().bill(bill.id)!!.isPaid)
        repo.unpayBill(bill.id)
        assertEquals(100_000L, db.dao().account(cashId)!!.balance)
        assertFalse(db.dao().bill(bill.id)!!.isPaid)
        assertTrue(db.dao().allTransactions().isEmpty())
    }

    @Test fun payingInstallmentBillUsesItsInstallmentAmount() = runTest {
        val installment = BillEntity("bill-2", "Kredivo", "Barang", 33_334, 1_700_000_000_000, 3, 3)
        db.dao().putBill(installment)
        repo.payBill(installment.id, cashId)
        assertEquals(66_666L, db.dao().account(cashId)!!.balance)
        assertEquals(33_334L, db.dao().transaction(db.dao().bill(installment.id)!!.paymentTransactionId!!)!!.amount)
    }

    @Test fun payDebtPiutangAddsMoneyAndUtangDeductsMoney() = runTest {
        repo.addDebt("piutang", "A", "Pinjam", 20_000)
        repo.addDebt("utang", "B", "Pinjam", 15_000)
        val debts = db.dao().allDebts()
        repo.payDebt(debts.first { it.type == "piutang" }.id, cashId, 20_000)
        repo.payDebt(debts.first { it.type == "utang" }.id, cashId, 10_000)
        assertEquals(110_000L, db.dao().account(cashId)!!.balance)
        assertEquals(10_000L, db.dao().allDebts().first { it.type == "utang" }.remaining)
    }

    @Test fun deleteTransactionReversesExpenseAndIncomeWithoutNegativeBalance() = runTest {
        repo.addTransaction("expense", 10_000, "Makan", cashId, 1_700_000_000_000, "")
        val expense = db.dao().allTransactions().single()
        repo.deleteTransaction(expense.id)
        assertEquals(100_000L, db.dao().account(cashId)!!.balance)
        assertTrue(db.dao().allTransactions().isEmpty())
        repo.addTransaction("income", 5_000, "Bonus", cashId, 1_700_000_000_001, "")
        val income = db.dao().allTransactions().single()
        repo.addTransaction("expense", 105_000, "Belanja", cashId, 1_700_000_000_002, "")
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repo.deleteTransaction(income.id) } }
        assertEquals(100_000L, db.dao().account(cashId)!!.balance)
        assertEquals(2, db.dao().allTransactions().size)
    }

    @Test fun deleteTransferRemovesBothLegsAndRestoresBothAccounts() = runTest {
        repo.addAccount("Bank", "Bank", 0)
        val bank = db.dao().allAccounts().first { it.name == "Bank" }
        repo.transfer(cashId, bank.id, 10_000)
        repo.deleteTransaction(db.dao().allTransactions().first().id)
        assertEquals(100_000L, db.dao().account(cashId)!!.balance)
        assertEquals(0L, db.dao().account(bank.id)!!.balance)
        assertTrue(db.dao().allTransactions().isEmpty())
    }

    @Test fun investmentContributionWithdrawalAndDeleteRestoreAccountBalances() = runTest {
        repo.addInvestment("Emergency", null)
        val asset = db.dao().allInvestments().single()
        repo.contribute(asset.id, cashId, 20_000)
        assertEquals(80_000L, db.dao().account(cashId)!!.balance)
        assertEquals(20_000L, db.dao().allInvestments().single().value)
        repo.withdraw(asset.id, cashId, 5_000)
        assertEquals(85_000L, db.dao().account(cashId)!!.balance)
        assertEquals(15_000L, db.dao().allInvestments().single().value)
        repo.deleteInvestment(asset.id)
        assertEquals(100_000L, db.dao().account(cashId)!!.balance)
        assertTrue(db.dao().allInvestments().single().archived)
    }

    @Test fun recalculateBalancesUsesOpeningBalanceAndTransactionHistory() = runTest {
        repo.addTransaction("expense", 10_000, "Makan", cashId, 1_700_000_000_000, "")
        val current = db.dao().account(cashId)!!
        db.dao().putAccount(current.copy(balance = 1L))
        repo.recalculateBalances()
        assertEquals(90_000L, db.dao().account(cashId)!!.balance)
    }



    @Test fun deletingDebtPaymentReversesAccountAndRestoresDebtOutstandingAmount() = runTest {
        repo.addDebt("piutang", "Teman", "Pinjaman", 25_000)
        val debt = db.dao().allDebts().single()
        repo.payDebt(debt.id, cashId, 10_000)
        val payment = db.dao().allTransactions().single()
        assertEquals(110_000L, db.dao().account(cashId)!!.balance)
        repo.deleteTransaction(payment.id)
        assertEquals(100_000L, db.dao().account(cashId)!!.balance)
        assertEquals(0L, db.dao().allDebts().single().paidAmount)
        assertEquals(25_000L, db.dao().allDebts().single().remaining)
    }

    @Test fun processDueSalarySchedulesCatchesUpAndIsIdempotent() = runTest {
        val start = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.JANUARY, 5, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        db.dao().putSalary(SalaryScheduleEntity("salary-1", cashId, 10_000, "daily", true, null, "Gaji", start, 0))
        val now = start + 2L * 86_400_000L + 60_000L
        val first = repo.processDueSchedules(now, 30)
        assertEquals(3, first.salaryRuns.size)
        assertEquals(130_000L, db.dao().account(cashId)!!.balance)
        val second = repo.processDueSchedules(now, 30)
        assertTrue(second.salaryRuns.isEmpty())
        assertEquals(3, db.dao().allTransactions().size)
    }

    @Test fun processDueInvestmentScheduleReportsInsufficientBalanceWithoutPosting() = runTest {
        val start = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.JANUARY, 5, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        repo.addInvestment("Scheduled", null)
        val asset = db.dao().allInvestments().single()
        val day = java.util.Calendar.getInstance().apply { timeInMillis = start }.get(java.util.Calendar.DAY_OF_WEEK)
        db.dao().putSchedule(ScheduledInvestmentEntity("schedule-1", asset.id, cashId, 200_000, day, true, null, start))
        val result = repo.processDueSchedules(start + 60_000L, 30)
        assertEquals(1, result.failures.size)
        assertEquals(100_000L, db.dao().account(cashId)!!.balance)
        assertTrue(db.dao().allTransactions().isEmpty())
        assertEquals(0L, db.dao().allInvestments().single().value)
    }

    @Test fun importInvalidBackupLeavesExistingDatabaseUnchanged() = runTest {
        repo.addTransaction("expense", 10_000, "Makan", cashId, 1_700_000_000_000, "")
        val before = repo.snapshot()
        val invalid = BackupData(
            accounts = listOf(AccountEntity("bad", "")),
            transactions = emptyList(), bills = emptyList(), investments = emptyList(),
            contributions = emptyList(), debts = emptyList()
        )
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repo.importJson(BackupCodec.encode(invalid)) } }
        assertEquals(before, repo.snapshot())
    }


    @Test fun importFailureAfterClearingAndPartialInsertRollsBackEntireDatabase() = runTest {
        repo.addTransaction("expense", 10_000, "Makan", cashId, 1_700_000_000_000, "Existing")
        val before = repo.snapshot()
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_import_transaction BEFORE INSERT ON transactions BEGIN SELECT RAISE(ABORT, 'forced test failure'); END"
        )
        val replacement = BackupData(
            accounts = listOf(AccountEntity("replacement", "Replacement", balance = 50_000, openingBalance = 50_000)),
            transactions = listOf(TransactionEntity("replacement-tx", "expense", 1_000, "Test", "replacement", 1_700_000_000_001)),
            bills = emptyList(), investments = emptyList(), contributions = emptyList(), debts = emptyList()
        )
        assertThrows(RuntimeException::class.java) {
            kotlinx.coroutines.runBlocking { repo.importJson(BackupCodec.encode(replacement)) }
        }
        assertEquals(before, repo.snapshot())
    }

    @Test fun importValidBackupReplacesAllExistingData() = runTest {
        val backup = BackupData(
            accounts = listOf(AccountEntity("restored", "Restored", balance = 42_000, openingBalance = 42_000)),
            transactions = emptyList(), bills = emptyList(), investments = emptyList(),
            contributions = emptyList(), debts = emptyList()
        )
        repo.importJson(BackupCodec.encode(backup))
        assertEquals(listOf("restored"), db.dao().allAccounts().map { it.id })
    }
}

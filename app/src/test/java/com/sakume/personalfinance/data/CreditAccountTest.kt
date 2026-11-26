package com.sakume.personalfinance.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CreditAccountTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: FinanceRepository
    private lateinit var cashId: String
    private val t0 = 1_700_000_000_000L

    @Before fun setUp() {
        runBlocking {
            db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java
            ).allowMainThreadQueries().addCallback(AppDatabase.SEED_ON_CREATE).build()
            repo = FinanceRepository(db)
            repo.addAccount("Cash", "Cash", 100_000L)
            cashId = db.dao().allAccounts().single().id
        }
    }

    @After fun tearDown() { if (::db.isInitialized) db.close() }

    private suspend fun addCredit(name: String = "PayLater", limit: Long? = 500_000L, owed: Long = 0L): AccountEntity {
        repo.addAccount(name, ACCOUNT_TYPE_CREDIT, owed, limit)
        return db.dao().allAccounts().first { it.name == name }
    }

    private suspend fun balanceOf(id: String): Long = db.dao().account(id)!!.balance

    private fun rejects(block: suspend () -> Unit) {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { block() } }
    }

    private fun emptyBackup(accounts: List<AccountEntity>) = BackupData(
        accounts = accounts,
        transactions = emptyList(), bills = emptyList(), investments = emptyList(),
        contributions = emptyList(), debts = emptyList()
    )

    @Test fun regularAccountCanHoldOnlyNonNegativeBalance() {
        val bank = AccountEntity("b", "Bank", "Bank", balance = 10_000)
        assertTrue(bank.canHold(0))
        assertTrue(bank.canHold(10_000))
        assertFalse(bank.canHold(-1))
        assertFalse(bank.isCredit)
        assertEquals(0L, bank.owed)
        assertNull(bank.availableCredit)
    }

    @Test fun creditAccountHonoursLimitOrIsUnboundedWithoutLimit() {
        val limited = AccountEntity("c", "Card", ACCOUNT_TYPE_CREDIT, creditLimit = 500_000)
        assertTrue(limited.canHold(0))
        assertTrue(limited.canHold(-500_000))
        assertFalse(limited.canHold(-500_001))
        val unbounded = AccountEntity("u", "Card", ACCOUNT_TYPE_CREDIT, creditLimit = null)
        assertTrue(unbounded.canHold(-50_000_000))
    }

    @Test fun owedAndAvailableCreditFollowBalanceAndLimit() {
        val card = AccountEntity("c", "Card", ACCOUNT_TYPE_CREDIT, balance = -150_000, creditLimit = 500_000)
        assertEquals(150_000L, card.owed)
        assertEquals(350_000L, card.availableCredit)
        val overpaid = card.copy(balance = 20_000)
        assertEquals(0L, overpaid.owed)
        assertEquals(500_000L, overpaid.availableCredit)
        assertNull(card.copy(creditLimit = null).availableCredit)
    }

    @Test fun netWorthSubtractsCreditObligations() {
        val accounts = listOf(
            AccountEntity("a", "Cash", "Cash", balance = 100_000),
            AccountEntity("b", "Bank", "Bank", balance = 400_000),
            AccountEntity("c", "Card", ACCOUNT_TYPE_CREDIT, balance = -150_000, creditLimit = 500_000)
        )
        assertEquals(350_000L, accounts.netWorth())
        assertEquals(150_000L, accounts.creditOwedTotal())
        assertEquals(500_000L, accounts.filterNot { it.isCredit }.netWorth())
    }

    @Test fun addCreditAccountStoresOwedAsNegativeOpeningBalance() = runTest {
        repo.addAccount("PayLater", "kredit", 120_000, 500_000)
        val card = db.dao().allAccounts().first { it.name == "PayLater" }
        assertEquals(ACCOUNT_TYPE_CREDIT, card.type)
        assertEquals(-120_000L, card.balance)
        assertEquals(-120_000L, card.openingBalance)
        assertEquals(500_000L, card.creditLimit)
        assertEquals(380_000L, card.availableCredit)
    }

    @Test fun addCreditAccountRejectsInvalidLimitOrOwedAboveLimit() = runTest {
        rejects { repo.addAccount("A", ACCOUNT_TYPE_CREDIT, 0, 0) }
        rejects { repo.addAccount("B", ACCOUNT_TYPE_CREDIT, 0, -1) }
        rejects { repo.addAccount("C", ACCOUNT_TYPE_CREDIT, 600_000, 500_000) }
        rejects { repo.addAccount("D", ACCOUNT_TYPE_CREDIT, -1, 500_000) }
        assertEquals(listOf("Cash"), db.dao().allAccounts().map { it.name })
    }

    @Test fun addNonCreditAccountRejectsLimitAndNegativeOpening() = runTest {
        rejects { repo.addAccount("Bank", "Bank", 0, 100_000) }
        rejects { repo.addAccount("Bank", "Bank", -1) }
        assertEquals(listOf("Cash"), db.dao().allAccounts().map { it.name })
    }

    @Test fun addCreditAccountWithoutLimitIsAllowed() = runTest {
        val card = addCredit(limit = null, owed = 50_000)
        assertNull(card.creditLimit)
        assertEquals(-50_000L, card.balance)
    }

    @Test fun expenseOnCreditWithinLimitSucceedsAndGoesNegative() = runTest {
        val card = addCredit()
        repo.addTransaction("expense", 300_000, CategoryIds.BELANJA, card.id, t0, "Beli")
        assertEquals(-300_000L, balanceOf(card.id))
        repo.addTransaction("expense", 200_000, CategoryIds.BELANJA, card.id, t0 + 1, "Beli lagi")
        assertEquals(-500_000L, balanceOf(card.id))
    }

    @Test fun expenseOnCreditBeyondLimitIsRejectedAtomically() = runTest {
        val card = addCredit()
        repo.addTransaction("expense", 300_000, CategoryIds.BELANJA, card.id, t0, "Beli")
        rejects { repo.addTransaction("expense", 200_001, CategoryIds.BELANJA, card.id, t0 + 1, "Terlalu besar") }
        assertEquals(-300_000L, balanceOf(card.id))
        assertEquals(1, db.dao().allTransactions().size)
    }

    @Test fun expenseOnCreditWithoutLimitIsNotCapped() = runTest {
        val card = addCredit(limit = null)
        repo.addTransaction("expense", 10_000_000, CategoryIds.BELANJA, card.id, t0, "Besar")
        assertEquals(-10_000_000L, balanceOf(card.id))
    }

    @Test fun regularAccountStillRejectsOverdraftNextToCreditAccount() = runTest {
        addCredit()
        rejects { repo.addTransaction("expense", 100_001, CategoryIds.MAKAN, cashId, t0, "") }
        assertEquals(100_000L, balanceOf(cashId))
        assertTrue(db.dao().allTransactions().isEmpty())
    }

    @Test fun incomeOnCreditReducesObligation() = runTest {
        val card = addCredit(owed = 200_000)
        repo.addTransaction("income", 50_000, CategoryIds.GAJI, card.id, t0, "Cashback")
        assertEquals(-150_000L, balanceOf(card.id))
    }

    @Test fun transferFromCreditRespectsLimit() = runTest {
        val card = addCredit(limit = 100_000)
        repo.transfer(card.id, cashId, 60_000)
        assertEquals(-60_000L, balanceOf(card.id))
        assertEquals(160_000L, balanceOf(cashId))
        rejects { repo.transfer(card.id, cashId, 40_001) }
        assertEquals(-60_000L, balanceOf(card.id))
        assertEquals(160_000L, balanceOf(cashId))
        assertEquals(2, db.dao().allTransactions().size)
    }

    @Test fun transferToCreditPaysDownObligation() = runTest {
        val card = addCredit(owed = 70_000)
        repo.transfer(cashId, card.id, 50_000)
        assertEquals(-20_000L, balanceOf(card.id))
        assertEquals(50_000L, balanceOf(cashId))
    }

    @Test fun transferFromRegularAccountStillRejectsInsufficientFunds() = runTest {
        val card = addCredit()
        rejects { repo.transfer(cashId, card.id, 100_001) }
        assertEquals(100_000L, balanceOf(cashId))
        assertEquals(0L, balanceOf(card.id))
    }

    @Test fun payBillFromCreditRespectsLimit() = runTest {
        val card = addCredit(limit = 100_000)
        val ok = BillEntity("bill-ok", "PLN", "Listrik", 80_000, t0)
        val tooBig = BillEntity("bill-big", "PDAM", "Air", 30_000, t0)
        db.dao().putBills(listOf(ok, tooBig))
        repo.payBill(ok.id, card.id)
        assertEquals(-80_000L, balanceOf(card.id))
        rejects { repo.payBill(tooBig.id, card.id) }
        assertEquals(-80_000L, balanceOf(card.id))
        assertFalse(db.dao().bill(tooBig.id)!!.isPaid)
        repo.unpayBill(ok.id)
        assertEquals(0L, balanceOf(card.id))
    }

    @Test fun payBillsFromCreditChecksCombinedTotalAgainstLimit() = runTest {
        val card = addCredit(limit = 100_000)
        val a = BillEntity("a", "PLN", "Listrik", 60_000, t0)
        val b = BillEntity("b", "PDAM", "Air", 50_000, t0)
        db.dao().putBills(listOf(a, b))
        rejects { repo.payBills(listOf(a, b), card.id) }
        assertEquals(0L, balanceOf(card.id))
        repo.payBills(listOf(a), card.id)
        assertEquals(-60_000L, balanceOf(card.id))
    }

    @Test fun payUtangFromCreditRespectsLimit() = runTest {
        val card = addCredit(limit = 100_000)
        repo.addDebt("utang", "Budi", "Pinjam", 150_000)
        val debt = db.dao().allDebts().single()
        rejects { repo.payDebt(debt.id, card.id, 100_001) }
        assertEquals(0L, balanceOf(card.id))
        repo.payDebt(debt.id, card.id, 100_000)
        assertEquals(-100_000L, balanceOf(card.id))
    }

    @Test fun piutangPaymentIntoCreditReducesObligation() = runTest {
        val card = addCredit(owed = 100_000)
        repo.addDebt("piutang", "Ani", "Pinjam", 40_000)
        val debt = db.dao().allDebts().single()
        repo.payDebt(debt.id, card.id, 40_000)
        assertEquals(-60_000L, balanceOf(card.id))
    }

    @Test fun contributeFromCreditRespectsLimitIncludingAdminFee() = runTest {
        val card = addCredit(limit = 100_000)
        repo.addInvestment("Reksa", null)
        val asset = db.dao().allInvestments().single()
        rejects { repo.contribute(asset.id, card.id, 100_000, 1) }
        assertEquals(0L, balanceOf(card.id))
        assertEquals(0L, db.dao().allInvestments().single().value)
        repo.contribute(asset.id, card.id, 99_000, 1_000)
        assertEquals(-100_000L, balanceOf(card.id))
    }

    @Test fun updateTransactionOnCreditRespectsLimit() = runTest {
        val card = addCredit()
        repo.addTransaction("expense", 400_000, CategoryIds.BELANJA, card.id, t0, "Beli")
        val tx = db.dao().allTransactions().single()
        rejects { repo.updateTransaction(tx.id, 500_001, CategoryIds.BELANJA, "Beli", t0, card.id) }
        assertEquals(-400_000L, balanceOf(card.id))
        repo.updateTransaction(tx.id, 450_000, CategoryIds.BELANJA, "Beli", t0, card.id)
        assertEquals(-450_000L, balanceOf(card.id))
    }

    @Test fun updateTransactionMovingExpenseOntoCreditRespectsLimit() = runTest {
        val card = addCredit(limit = 50_000)
        repo.addTransaction("expense", 80_000, CategoryIds.BELANJA, cashId, t0, "Beli")
        val tx = db.dao().allTransactions().single()
        rejects { repo.updateTransaction(tx.id, 80_000, CategoryIds.BELANJA, "Beli", t0, card.id) }
        assertEquals(20_000L, balanceOf(cashId))
        assertEquals(0L, balanceOf(card.id))
    }

    @Test fun updateTransactionOnRegularAccountStillRejectsNegativeResult() = runTest {
        addCredit()
        repo.addTransaction("expense", 50_000, CategoryIds.MAKAN, cashId, t0, "")
        val tx = db.dao().allTransactions().single()
        rejects { repo.updateTransaction(tx.id, 100_001, CategoryIds.MAKAN, "", t0, cashId) }
        assertEquals(50_000L, balanceOf(cashId))
    }

    @Test fun deleteExpenseOnCreditRestoresBalance() = runTest {
        val card = addCredit()
        repo.addTransaction("expense", 120_000, CategoryIds.BELANJA, card.id, t0, "Beli")
        repo.deleteTransaction(db.dao().allTransactions().single().id)
        assertEquals(0L, balanceOf(card.id))
    }

    @Test fun deletePaymentIntoCreditIsRejectedWhenItWouldBreachLimit() = runTest {
        val card = addCredit()
        repo.addTransaction("expense", 500_000, CategoryIds.BELANJA, card.id, t0, "Beli")
        repo.addTransaction("income", 100_000, CategoryIds.GAJI, card.id, t0 + 1, "Bayar")
        val payment = db.dao().allTransactions().first { it.type == "income" }
        repo.addTransaction("expense", 100_000, CategoryIds.BELANJA, card.id, t0 + 2, "Beli lagi")
        assertEquals(-500_000L, balanceOf(card.id))
        rejects { repo.deleteTransaction(payment.id) }
        assertEquals(-500_000L, balanceOf(card.id))
        assertEquals(3, db.dao().allTransactions().size)
    }

    @Test fun deleteTransferLegIntoCreditAndBackOutRestoresBothAccounts() = runTest {
        val card = addCredit(owed = 70_000)
        repo.transfer(cashId, card.id, 50_000)
        val group = db.dao().allTransactions().first().id
        repo.deleteTransaction(group)
        assertEquals(100_000L, balanceOf(cashId))
        assertEquals(-70_000L, balanceOf(card.id))
        assertTrue(db.dao().allTransactions().isEmpty())
    }

    @Test fun recalculateBalancesKeepsNegativeOpeningOfCreditAccount() = runTest {
        val card = addCredit(owed = 100_000)
        repo.addTransaction("expense", 50_000, CategoryIds.BELANJA, card.id, t0, "Beli")
        db.dao().putAccount(db.dao().account(card.id)!!.copy(balance = 0))
        repo.recalculateBalances()
        assertEquals(-150_000L, balanceOf(card.id))
    }

    private fun scheduleStart(): Long = Calendar.getInstance().apply {
        set(2026, Calendar.JANUARY, 5, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test fun scheduledInvestmentFromCreditFailsBeyondLimitWithoutPosting() = runTest {
        val card = addCredit(limit = 50_000)
        val start = scheduleStart()
        repo.addInvestment("Scheduled", null)
        val asset = db.dao().allInvestments().single()
        val day = Calendar.getInstance().apply { timeInMillis = start }.get(Calendar.DAY_OF_WEEK)
        db.dao().putSchedule(ScheduledInvestmentEntity("schedule-1", asset.id, card.id, 200_000, day, true, null, start))
        val result = repo.processDueSchedules(start + 60_000L, 30)
        assertEquals(1, result.failures.size)
        assertEquals(0L, balanceOf(card.id))
        assertTrue(db.dao().allTransactions().isEmpty())
    }

    @Test fun scheduledInvestmentFromCreditWithinLimitPostsAndGoesNegative() = runTest {
        val card = addCredit(limit = 50_000)
        val start = scheduleStart()
        repo.addInvestment("Scheduled", null)
        val asset = db.dao().allInvestments().single()
        val day = Calendar.getInstance().apply { timeInMillis = start }.get(Calendar.DAY_OF_WEEK)
        db.dao().putSchedule(ScheduledInvestmentEntity("schedule-1", asset.id, card.id, 30_000, day, true, null, start))
        val result = repo.processDueSchedules(start + 60_000L, 30)
        assertTrue(result.failures.isEmpty())
        assertEquals(-30_000L, balanceOf(card.id))
    }

    @Test fun deleteAccountRejectsCreditAccountWithOutstandingBalance() = runTest {
        val card = addCredit(owed = 10_000)
        rejects { repo.deleteAccount(card.id) }
        assertEquals(1, db.dao().allAccounts().count { it.id == card.id })
    }

    @Test fun backupRoundTripCarriesCreditTypeLimitAndNegativeBalance() = runTest {
        val card = addCredit(limit = 500_000, owed = 120_000)
        val json = repo.exportJson()
        val decoded = BackupCodec.decode(json)
        val exported = decoded.accounts.first { it.id == card.id }
        assertEquals(500_000L, exported.creditLimit)
        assertEquals(ACCOUNT_TYPE_CREDIT, exported.type)
        repo.importJson(json)
        val restored = db.dao().account(card.id)!!
        assertEquals(500_000L, restored.creditLimit)
        assertEquals(-120_000L, restored.balance)
        assertEquals(-120_000L, restored.openingBalance)
        assertTrue(restored.isCredit)
    }

    @Test fun backupFromOlderFormatWithoutCreditLimitStillRestores() = runTest {
        val old = emptyBackup(
            listOf(AccountEntity("restored", "Restored", "Bank", balance = 42_000, openingBalance = 42_000))
        ).copy(meta = BackupMeta(format = 3, createdAtMillis = t0, appVersion = "1.4.0"))
        val json = BackupCodec.encode(old).replace(",\"creditLimit\":null", "")
        assertFalse(json.contains("creditLimit"))
        repo.importJson(json)
        val restored = db.dao().account("restored")!!
        assertNull(restored.creditLimit)
        assertEquals(42_000L, restored.balance)
        assertEquals(4, BackupCodec.FORMAT_VERSION)
    }

    @Test fun importDropsCreditLimitFromNonCreditAccount() = runTest {
        val backup = emptyBackup(
            listOf(AccountEntity("b", "Bank", "Bank", balance = 1_000, openingBalance = 1_000, creditLimit = 100_000))
        )
        repo.importJson(BackupCodec.encode(backup))
        assertNull(db.dao().account("b")!!.creditLimit)
    }

    @Test fun importNormalizesCreditTypeSpelling() = runTest {
        val backup = emptyBackup(
            listOf(AccountEntity("c", "Card", "kredit", balance = -5_000, openingBalance = -5_000, creditLimit = 100_000))
        )
        repo.importJson(BackupCodec.encode(backup))
        val restored = db.dao().account("c")!!
        assertEquals(ACCOUNT_TYPE_CREDIT, restored.type)
        assertEquals(100_000L, restored.creditLimit)
    }

    @Test fun importRejectsCreditAccountWithNonPositiveLimitAndKeepsExistingData() = runTest {
        repo.addTransaction("expense", 10_000, CategoryIds.MAKAN, cashId, t0, "")
        val before = repo.snapshot()
        val invalid = emptyBackup(
            listOf(AccountEntity("c", "Card", ACCOUNT_TYPE_CREDIT, creditLimit = 0))
        )
        rejects { repo.importJson(BackupCodec.encode(invalid)) }
        assertEquals(before, repo.snapshot())
    }

    @Test fun importStillAllowsNegativeBalanceOnCreditAccountBeyondLimit() = runTest {
        val backup = emptyBackup(
            listOf(AccountEntity("c", "Card", ACCOUNT_TYPE_CREDIT, balance = -900_000, openingBalance = -900_000, creditLimit = 500_000))
        )
        repo.importJson(BackupCodec.encode(backup))
        assertEquals(-900_000L, db.dao().account("c")!!.balance)
    }
}

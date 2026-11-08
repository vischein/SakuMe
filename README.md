# SakuMe — Personal Finance (Android)

Project Android native Kotlin + Jetpack Compose + Material 3 + Room, minSdk 26, compile/target SDK 36, JVM 17.

## Features included
- Multi-account balances and transaction history.
- Income/expense and internal transfer balance accounting.
- Provider bills with installment splitting, SPayLater due-date rule, urgency, grouping, pay one/pay month/pay multiple and paid history.
- Investment assets, account contributions linked to expense transaction, refunds to original contributing accounts when deleted.
- Receivables/payables with paid state and unpaid totals.
- JSON backup model/codec and repository import/export methods.

## Important
- Package/application ID: `com.sakume.personalfinance`. Rename only if desired.
- UI is based on the supplied reference: monochrome charcoal balance card, compact metric cards, clean light background and bottom navigation.
- This is a local-first starter project; no bank integration, cloud sync, authentication, or encryption is configured.
- The JSON export/import functions are implemented in the repository. Connecting them to Android's document picker UI is the next integration step.
- Account transfers are stored as paired linked transaction records. They do not count as income/expense.
- Deleting an investment refunds contributions to the exact originating accounts and archives the asset so contribution audit data remains recoverable.

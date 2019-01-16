// !LANGUAGE: +ContextualEffects +UseCallsInPlaceEffect +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE_ERROR -CONTRACT_NOT_ALLOWED
// !RENDER_DIAGNOSTICS_MESSAGES

import kotlin.contracts.*
import org.jetbrains.kotlin.contracts.contextual.*
import org.jetbrains.kotlin.contracts.contextual.transactions.*

class Transaction {
    fun start() {
        contract {
            starts(OpenedTransaction(this@Transaction))
        }
        // open transaction
    }

    fun setData() {
        contract {
            requires(OpenedTransaction(this@Transaction))
        }
        // somethig useful
    }

    fun commit() {
        contract {
            closes(OpenedTransaction(this@Transaction))
        }
        // commit transaction
    }
}

// ---------------- TESTS ----------------

fun test_1(b: Boolean) {
    val transaction = Transaction()
    transaction.start()
    if (b) {
        transaction.setData()
    }
    transaction.commit()
}

fun test_2(b: Boolean) {
    val transaction = Transaction()
    if (b) {
        transaction.start()
    }
    transaction.<!CONTEXTUAL_EFFECT_WARNING(transaction is not opened)!>setData()<!>
    transaction.<!CONTEXTUAL_EFFECT_WARNING(transaction is not opened)!>commit()<!>
}

fun test_3(b: Boolean) {
    val transaction = Transaction()
    if (b) {
        transaction.start()
    } else {
        transaction.start()
    }
    transaction.setData()
    transaction.commit()
}
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

fun test_1() {
    val transaction1 = Transaction()
    val transaction2 = Transaction()

    transaction1.start()
    transaction2.start()
    transaction1.setData()
    transaction2.setData()
    transaction1.commit()
    transaction2.commit()
}

fun test_2() {
    val transaction1 = Transaction()
    val transaction2 = Transaction()

    transaction1.start()
    transaction1.setData()
    transaction2.<!CONTEXTUAL_EFFECT_WARNING(transaction2 is not opened)!>setData()<!>
    transaction1.commit()
    transaction2.<!CONTEXTUAL_EFFECT_WARNING(transaction2 is not opened)!>commit()<!>
}

fun test_3() {
    val transaction1 = Transaction()
    val transaction2 = Transaction()

    transaction1.<!CONTEXTUAL_EFFECT_WARNING(transaction1 is not opened)!>setData()<!>
    transaction2.<!CONTEXTUAL_EFFECT_WARNING(transaction2 is not opened)!>setData()<!>
    transaction1.<!CONTEXTUAL_EFFECT_WARNING(transaction1 is not opened)!>commit()<!>
    transaction2.<!CONTEXTUAL_EFFECT_WARNING(transaction2 is not opened)!>commit()<!>
}
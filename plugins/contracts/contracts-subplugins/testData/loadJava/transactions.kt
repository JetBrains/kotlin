// !LANGUAGE: +ContextualEffects +UseCallsInPlaceEffect +AllowContractsForCustomFunctions +UseReturnsEffect +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

package test

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
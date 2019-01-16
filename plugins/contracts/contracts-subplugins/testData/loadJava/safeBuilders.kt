// !LANGUAGE: +ContextualEffects +UseCallsInPlaceEffect +AllowContractsForCustomFunctions +UseReturnsEffect +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

package test

import kotlin.contracts.*
import org.jetbrains.kotlin.contracts.contextual.*
import org.jetbrains.kotlin.contracts.contextual.safebuilders.*

data class X(val x: Int)

class XBuilder {
    private var x_: Int? = null
    fun setX(value: Int = 0) {
        contract {
            provides(Calls(::setX, this@XBuilder))
        }
        x_ = value
    }

    fun buildX() = X(x_!!)
}

fun buildX(init: XBuilder.() -> Unit): X {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        expectsTo(init, CallKind(XBuilder::setX, InvocationKind.EXACTLY_ONCE, receiverOf(init)))
    }
    val builder = XBuilder()
    builder.init()
    return builder.buildX()
}
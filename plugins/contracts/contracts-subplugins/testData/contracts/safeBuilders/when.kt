// !LANGUAGE: +ContextualEffects +UseCallsInPlaceEffect +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE_ERROR -DATA_CLASS_WITHOUT_PARAMETERS -CONTRACT_NOT_ALLOWED
// !RENDER_DIAGNOSTICS_MESSAGES

import kotlin.contracts.*
import org.jetbrains.kotlin.contracts.contextual.*
import org.jetbrains.kotlin.contracts.contextual.safebuilders.*

data class XY(/*...*/)

class XYBuilder {
    private var x_: Int? = null
    fun setValX(value: Int = 0) {
        contract {
            provides(Calls(::setValX, this@XYBuilder))
        }
        x_ = value
    }

    private var y_: Int? = null
    fun setDefaultValY(value: Int = 0) {
        contract {
            provides(Calls(::setDefaultValY, this@XYBuilder))
        }
        y_ = value
    }

    fun buildXY() = XY(/*...*/)
}

fun buildXY(init: XYBuilder.() -> Unit): XY {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        expectsTo(init, CallKind(XYBuilder::setValX, InvocationKind.EXACTLY_ONCE, receiverOf(init)))
        expectsTo(init, CallKind(XYBuilder::setDefaultValY, InvocationKind.AT_MOST_ONCE, receiverOf(init)))
    }
    val builder = XYBuilder()
    builder.init()
    return builder.buildXY()
}

data class Z(/*...*/)

class ZBuilder {
    private var z_: Int? = null
    fun setVarZ(value: Int = 0) {
        contract {
            provides(Calls(::setVarZ, this@ZBuilder))
        }
        z_ = value
    }

    fun buildZ() = Z(/*...*/)
}

fun buildZ(init: ZBuilder.() -> Unit): Z {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        expectsTo(init, CallKind(ZBuilder::setVarZ, InvocationKind.AT_LEAST_ONCE, receiverOf(init)))
    }
    val builder = ZBuilder()
    builder.init()
    return builder.buildZ()
}

// ---------------- TESTS ----------------

fun test_1(x: Int) {
    buildXY {
        setValX()
        when (x) {
            in 1..2 -> setDefaultValY() // once
            in 3..4 -> if (x == 3) setDefaultValY() // at least once
            else -> {} // zero
        } // must be at least once
    }
}

fun test_2(x: Int) {
    buildXY {
        when (x) {
            in 1..2 -> setValX()
            in 3..4 -> setValX()
            else -> setValX()
        }
    }
}

fun test_3(x: Int) {
    buildZ <!CONTEXTUAL_EFFECT_WARNING(setVarZ call mismatch: expected AT_LEAST_ONCE, actual UNKNOWN)!>{
        when (x) {
            in 1..2 -> { // at least once
                setVarZ()
                setVarZ()
            }
            in 3..4 -> setVarZ() // once
            else -> {} // zero
        } // must be unknown
    }<!>
}

fun test_4(x: Int) {
    buildZ <!CONTEXTUAL_EFFECT_WARNING(setVarZ call mismatch: expected AT_LEAST_ONCE, actual UNKNOWN)!>{
        when (x) {
            in 1..2 -> { // at least once
                setVarZ()
                setVarZ()
            }
            in 3..4 -> setVarZ() // once
            else -> { // unknown
                for (i in 5..6) {
                    setVarZ()
                }
            }
        } // must be unknown
    }<!>
}
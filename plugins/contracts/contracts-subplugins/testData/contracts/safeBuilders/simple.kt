// !LANGUAGE: +ContextualEffects +UseCallsInPlaceEffect +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE_ERROR -DATA_CLASS_WITHOUT_PARAMETERS -CONTRACT_NOT_ALLOWED
// !RENDER_DIAGNOSTICS_MESSAGES

import kotlin.contracts.*
import org.jetbrains.kotlin.contracts.contextual.*
import org.jetbrains.kotlin.contracts.contextual.safebuilders.*

data class XYZ(/*...*/)

class XYZBuilder {
    private var x_: Int? = null
    fun setValX(value: Int = 0) {
        contract {
            provides(Calls(::setValX, this@XYZBuilder))
        }
        x_ = value
    }

    private var y_: Int? = null
    fun setDefaultValY(value: Int = 0) {
        contract {
            provides(Calls(::setDefaultValY, this@XYZBuilder))
        }
        y_ = value
    }

    private var z_: Int? = null
    fun setVarZ(value: Int = 0) {
        contract {
            provides(Calls(::setVarZ, this@XYZBuilder))
        }
        z_ = value
    }

    fun buildXYZ() = XYZ(/*...*/)
}

fun buildXYZ(init: XYZBuilder.() -> Unit): XYZ {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        expectsTo(init, CallKind(XYZBuilder::setValX, InvocationKind.EXACTLY_ONCE, receiverOf(init)))
        expectsTo(init, CallKind(XYZBuilder::setDefaultValY, InvocationKind.AT_MOST_ONCE, receiverOf(init)))
        expectsTo(init, CallKind(XYZBuilder::setVarZ, InvocationKind.AT_LEAST_ONCE, receiverOf(init)))
    }
    val builder = XYZBuilder()
    builder.init()
    return builder.buildXYZ()
}

// ---------------- TESTS ----------------

fun test_1() {
    buildXYZ {
        setValX()
        setDefaultValY()
        setVarZ()
    }
}

fun test_2() {
    buildXYZ <!CONTEXTUAL_EFFECT_WARNING(setValX call mismatch: expected EXACTLY_ONCE, actual ZERO), CONTEXTUAL_EFFECT_WARNING(setVarZ call mismatch: expected AT_LEAST_ONCE, actual ZERO)!>{}<!>
}

fun test_3() {
    buildXYZ <!CONTEXTUAL_EFFECT_WARNING(setValX call mismatch: expected EXACTLY_ONCE, actual AT_LEAST_ONCE), CONTEXTUAL_EFFECT_WARNING(setDefaultValY call mismatch: expected AT_MOST_ONCE, actual AT_LEAST_ONCE)!>{
        setValX()
        setDefaultValY()
        setVarZ()

        setValX()
        setDefaultValY()
        setVarZ()
    }<!>
}
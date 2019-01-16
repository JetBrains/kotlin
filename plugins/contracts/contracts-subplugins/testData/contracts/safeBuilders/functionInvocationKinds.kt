// !LANGUAGE: +ContextualEffects +UseCallsInPlaceEffect +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE_ERROR -CONTRACT_NOT_ALLOWED
// !RENDER_DIAGNOSTICS_MESSAGES

import kotlin.contracts.*
import org.jetbrains.kotlin.contracts.contextual.*
import org.jetbrains.kotlin.contracts.contextual.safebuilders.*

data class A(val x: Int?)

class ABuilder {
    private var x_: Int? = null
    fun setX(value: Int = 0) {
        contract {
            provides(Calls(::setX, this@ABuilder))
        }
        x_ = value
    }

    fun buildA() = A(x_)
}

fun buildExactlyOnce(init: ABuilder.() -> Unit): A {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        expectsTo(init, CallKind(ABuilder::setX, InvocationKind.EXACTLY_ONCE, receiverOf(init)))
    }
    val builder = ABuilder()
    builder.init()
    return builder.buildA()
}

fun buildAtMostOnce(init: ABuilder.() -> Unit): A {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        expectsTo(init, CallKind(ABuilder::setX, InvocationKind.AT_MOST_ONCE, receiverOf(init)))
    }
    val builder = ABuilder()
    builder.init()
    return builder.buildA()
}

fun buildAtLeastOnce(init: ABuilder.() -> Unit): A {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        expectsTo(init, CallKind(ABuilder::setX, InvocationKind.AT_LEAST_ONCE, receiverOf(init)))
    }
    val builder = ABuilder()
    builder.init()
    return builder.buildA()
}

inline fun onceRun(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

inline fun maybeRun(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    // non determenistic
    val b = true
    if (b) {
        block()
    }
}

inline fun multipleRun(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    block()
    block()
}

// ---------------- TESTS ----------------

fun test_1() {
    buildExactlyOnce {
        onceRun {
            setX()
        }
    }
}

fun test_2() {
    buildExactlyOnce <!CONTEXTUAL_EFFECT_WARNING(setX call mismatch: expected EXACTLY_ONCE, actual AT_MOST_ONCE)!>{
        maybeRun {
            setX()
        }
    }<!>
}

fun test_3() {
    buildExactlyOnce <!CONTEXTUAL_EFFECT_WARNING(setX call mismatch: expected EXACTLY_ONCE, actual AT_LEAST_ONCE)!>{
        multipleRun {
            setX()
        }
    }<!>
}



fun test_4() {
    buildAtMostOnce {
        onceRun {
            setX()
        }
    }
}

fun test_5() {
    buildAtMostOnce {
        maybeRun {
            setX()
        }
    }
}

fun test_6() {
    buildAtMostOnce <!CONTEXTUAL_EFFECT_WARNING(setX call mismatch: expected AT_MOST_ONCE, actual AT_LEAST_ONCE)!>{
        multipleRun {
            setX()
        }
    }<!>
}



fun test_7() {
    buildAtLeastOnce {
        onceRun {
            setX()
        }
    }
}

fun test_8() {
    buildAtLeastOnce <!CONTEXTUAL_EFFECT_WARNING(setX call mismatch: expected AT_LEAST_ONCE, actual AT_MOST_ONCE)!>{
        maybeRun {
            setX()
        }
    }<!>
}

fun test_9() {
    buildAtLeastOnce {
        multipleRun {
            setX()
        }
    }
}



fun test_10() {
    buildAtLeastOnce {
        onceRun {
            setX()
            setX()
        }
    }
}

fun test_11() {
    buildAtLeastOnce <!CONTEXTUAL_EFFECT_WARNING(setX call mismatch: expected AT_LEAST_ONCE, actual UNKNOWN)!>{
        maybeRun {
            setX()
            setX()
        }
    }<!>
}

fun test_12() {
    buildAtLeastOnce {
        multipleRun {
            setX()
            setX()
        }
    }
}
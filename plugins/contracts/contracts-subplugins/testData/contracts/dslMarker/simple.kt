// !LANGUAGE: +ContextualEffects +UseCallsInPlaceEffect +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE_ERROR -DATA_CLASS_WITHOUT_PARAMETERS -CONTRACT_NOT_ALLOWED
// !RENDER_DIAGNOSTICS_MESSAGES

import kotlin.contracts.*
import org.jetbrains.kotlin.contracts.contextual.*
import org.jetbrains.kotlin.contracts.contextual.dslmarker.*

data class Foo(/*...*/)

data class Bar(/*...*/)

class BarBuilder {
    private var y_: Int? = null
    fun setY(value: Int = 0) {
        contract {
            requires(DslMarkers(this@BarBuilder))
        }
        y_ = value
    }

    fun create(): Bar = Bar(/*...*/)
}

class FooBuilder {
    private var x_: Int? = null
    fun setX(value: Int = 0) {
        contract {
            requires(DslMarkers(this@FooBuilder))
        }
        x_ = value
    }

    fun buildBar(init: BarBuilder.() -> Unit): Bar {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
            callsIn(init, DslMarkers(receiverOf(init)))
            requires(DslMarkers(this@FooBuilder))
        }
        val builder = BarBuilder()
        builder.init()
        return builder.create()
    }

    fun create() = Foo(/*...*/)
}

fun buildFoo(init: FooBuilder.() -> Unit): Foo {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        callsIn(init, DslMarkers(receiverOf(init)))
    }
    val builder = FooBuilder()
    builder.init()
    return builder.create()
}

// ---------------- TESTS ----------------

fun test_1() {
    buildFoo {
        setX()
        buildBar {
            setY()
        }
    }
}

fun test_2() {
    buildFoo {
        buildBar {
            <!CONTEXTUAL_EFFECT_WARNING(Call function in wrong scope)!>setX()<!>
            setY()
        }
    }
}
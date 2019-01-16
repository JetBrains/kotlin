// !LANGUAGE: +ContextualEffects +UseCallsInPlaceEffect +AllowContractsForCustomFunctions +UseReturnsEffect +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

package test

import kotlin.contracts.*
import org.jetbrains.kotlin.contracts.contextual.*
import org.jetbrains.kotlin.contracts.contextual.dslmarker.*

data class Foo(val x: Int)

data class Bar(val y: Int)

class BarBuilder {
    private var y_: Int? = null
    fun setY(value: Int = 0) {
        contract {
            requires(DslMarkers(this@BarBuilder))
        }
        y_ = value
    }

    fun create(): Bar = Bar(y_!!)
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

    fun create() = Foo(x_!!)
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
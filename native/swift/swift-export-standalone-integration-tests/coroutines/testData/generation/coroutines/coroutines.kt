// IGNORE_NATIVE: targetFamily=IOS
// IGNORE_NATIVE: targetFamily=TVOS
// IGNORE_NATIVE: targetFamily=WATCHOS
// IGNORE_NATIVE: target=macos_x64
// KIND: STANDALONE
// APPLE_ONLY_VALIDATION
// MODULE: main
// FILE: coroutines_demo.kt
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class Foo

fun demo(): Flow<Foo> = TODO()

val flowFoo: Flow<Foo> = TODO()

fun closure_returning_flow(i: (Flow<Foo>)->Unit): Unit = TODO()

fun demo_ft_produce(): suspend (Int) -> Int = TODO()

typealias AliasedFunctionType = (Float) -> Int

suspend fun produce_function_typealias(): AliasedFunctionType = TODO()

// MODULE: flow_overrides
// FILE: flow_overrides.kt
package namespace

import kotlinx.coroutines.flow.*

sealed interface I1 {
    interface I2: I1
}

open class Foo {
    open fun foo(): Flow<I1?> = TODO()
    open val voo: Flow<I1?> get() = TODO()
}

open class StateFoo: Foo() {
    override fun foo(): StateFlow<I1?> = TODO()
    override val voo: StateFlow<I1?> get() = TODO()
}

open class Bar: Foo() {
    override fun foo(): Flow<I1.I2> = TODO()
    override val voo: Flow<I1.I2> get() = TODO()
}

open class Zar: Foo() {
    override fun foo(): Flow<I1.I2?> = TODO()
    override val voo: Flow<I1.I2?> get() = TODO()
}

open class Nar: Foo() {
    override fun foo(): Flow<Nothing> = TODO()
    override val voo: Flow<Nothing> get() = TODO()
}
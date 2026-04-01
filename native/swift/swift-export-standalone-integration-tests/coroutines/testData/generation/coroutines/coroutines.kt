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

fun closure_returning_flow(i: (Flow<Foo>) -> Unit): Unit = TODO()

fun consume_flow(flow: Flow<Foo>): Unit = TODO()

fun produce_flow(): Flow<Int> = TODO()

fun produce_function(): suspend (Int) -> Int = TODO()

typealias AliasedFunctionType = (Float) -> Int

suspend fun produce_function_typealias(): AliasedFunctionType = TODO()

suspend fun produce_suspend_function(): suspend (Double) -> Int = TODO()

typealias AliasedAsyncFunctionType = suspend (Float) -> Long

suspend fun produce_suspend_function_typealias(): AliasedAsyncFunctionType = TODO()

fun accept_suspend_function_type(block: suspend () -> Int): Unit = TODO()

suspend fun returnUnit(): Unit = TODO()

fun returnSuspendUnit(): suspend () -> Unit = TODO()

suspend fun alwaysFails(): Nothing = TODO()

fun flowOfUnit(): Flow<Unit> = TODO()

fun flowOfNullableUnit(): Flow<Unit?> = TODO()

fun mutableStateFlowOfUnit(): MutableStateFlow<Unit> = TODO()

// FILE: coroutines_extra.kt

suspend fun returnsList(): List<String> = emptyList()

suspend fun retunsListOfSuspend(): List<suspend () -> Unit> = emptyList()

suspend fun returnsListOfSuspendNullables(): List<(suspend () -> Unit)?> = emptyList()

public fun interface FunctionalInterfaceWithSuspendFunction {
    public suspend fun emit()
}

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

open class SharedFoo: Foo() {
    override fun foo(): SharedFlow<I1?> = TODO()
    override val voo: SharedFlow<I1?> get() = TODO()
}

open class MutableSharedFoo: SharedFoo() {
    override fun foo(): MutableSharedFlow<I1?> = TODO()
    override val voo: MutableSharedFlow<I1?> get() = TODO()
}

open class StateFoo: SharedFoo() {
    override fun foo(): StateFlow<I1?> = TODO()
    override val voo: StateFlow<I1?> get() = TODO()
}

open class MutableStateFoo: StateFoo() {
    override fun foo(): MutableStateFlow<I1?> = TODO()
    override val voo: MutableStateFlow<I1?> get() = TODO()
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

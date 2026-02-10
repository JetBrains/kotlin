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

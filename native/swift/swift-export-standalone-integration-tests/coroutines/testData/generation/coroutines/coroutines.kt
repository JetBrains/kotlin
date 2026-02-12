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

fun produce_flow(): Flow<Int> = TODO()

fun produce_function(): suspend (Int) -> Int = TODO()

typealias AliasedFunctionType = (Float) -> Int

suspend fun produce_function_typealias(): AliasedFunctionType = TODO()

suspend fun produce_suspend_function(): suspend (Double) -> Int = TODO()

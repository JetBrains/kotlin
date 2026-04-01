// IGNORE_NATIVE: targetFamily=IOS
// IGNORE_NATIVE: targetFamily=TVOS
// IGNORE_NATIVE: targetFamily=WATCHOS
// IGNORE_NATIVE: target=macos_x64
// KIND: STANDALONE
// APPLE_ONLY_VALIDATION
// MODULE: main
// FILE: closures.kt
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun accept_suspend_function_type(block: suspend () -> Int): Int = TODO()
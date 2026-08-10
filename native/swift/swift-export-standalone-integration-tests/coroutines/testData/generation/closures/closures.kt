// KIND: STANDALONE
// APPLE_ONLY_VALIDATION
// MODULE: main
// FILE: closures.kt
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun accept_suspend_function_type(block: suspend () -> Int): Int = TODO()

fun accept_suspend_fun_with_context(block: suspend context(String) () -> Int): Int = TODO()

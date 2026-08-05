// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: suspend_cancellation_from_kotlin.kt

import kotlinx.coroutines.*

open class AsyncBase {
    open suspend fun greet(name: String): String = "Kotlin: $name"
}

// Drives a reverse-bridge suspend call under a Kotlin-side timeout, so a Kotlin timeout cancels the Swift
// override. Returns a sentinel instead of null so the Swift side can assert on it.
suspend fun callGreetWithTimeout(base: AsyncBase, name: String, timeoutMs: Long): String =
    withTimeoutOrNull(timeoutMs) { base.greet(name) } ?: "timed_out"

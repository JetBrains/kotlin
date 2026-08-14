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

// Cancellation has to reach the Swift override through an intermediate Kotlin frame: `describe()` is not
// overridden in Swift, so its itable slot stays unpatched and Kotlin's own default body runs; that body then
// calls `tag()`, which does go out through the patched itable slot into the Swift override that suspends.
interface AsyncDefaulter {
    suspend fun tag(): String
    suspend fun describe(): String = "default-describe(" + tag() + ")"
}
open class AsyncAnchor

suspend fun callAsyncDescribeWithTimeout(d: AsyncDefaulter, timeoutMs: Long): String =
    withTimeoutOrNull(timeoutMs) { d.describe() } ?: "timed_out"


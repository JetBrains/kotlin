// IGNORE_NATIVE: targetFamily=IOS
// IGNORE_NATIVE: targetFamily=TVOS
// IGNORE_NATIVE: targetFamily=WATCHOS
// IGNORE_NATIVE: target=macos_x64
// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: lib.kt

import kotlinx.coroutines.*

open class AsyncBase {
    open suspend fun greet(name: String): String = "Kotlin: $name"
    open suspend fun count(): Int = 42
}

suspend fun callGreet(base: AsyncBase, name: String): String = base.greet(name)
suspend fun callCount(base: AsyncBase): Int = base.count()

// Drives a reverse-bridge suspend call under a Kotlin-side timeout, so a Kotlin timeout cancels the
// Swift override that backs `greet`. Returns a sentinel instead of null so the Swift side can assert on it.
suspend fun callGreetWithTimeout(base: AsyncBase, name: String, timeoutMs: Long): String =
    withTimeoutOrNull(timeoutMs) { base.greet(name) } ?: "timed_out"

interface AsyncSpeaker {
    suspend fun speak(): String
}

open class AsyncSpeakerBase : AsyncSpeaker {
    override suspend fun speak(): String = "Kotlin speaks"
}

suspend fun callSpeak(s: AsyncSpeaker): String = s.speak()

class AsyncException(message: String) : RuntimeException(message)

open class AsyncThrower {
    open suspend fun boom(): String {
        throw AsyncException("kotlin-boom")
    }
}

suspend fun callBoom(t: AsyncThrower): String = t.boom()

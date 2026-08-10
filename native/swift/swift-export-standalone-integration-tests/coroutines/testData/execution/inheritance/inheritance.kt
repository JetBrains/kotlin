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

// Defaulted suspend interface method: a Swift class that inherits a Kotlin class and first-adopts this
// interface, without overriding `describe`, must inherit the Kotlin async default via the non-virtual
// ("_direct") forward async bridge, never recursing through its patched itable slot. The default's
// open self-call to the abstract `tag()` must reach the Swift override.
interface AsyncDefaulter {
    suspend fun tag(): String
    suspend fun describe(): String = "default-describe(" + tag() + ")"
}

suspend fun callAsyncDescribe(d: AsyncDefaulter): String = d.describe()
suspend fun callAsyncTag(d: AsyncDefaulter): String = d.tag()

class AsyncException(message: String) : RuntimeException(message)

open class AsyncThrower {
    open suspend fun boom(): String {
        throw AsyncException("kotlin-boom")
    }
}

suspend fun callBoom(t: AsyncThrower): String = t.boom()

open class AsyncVararg {
    open suspend fun join(vararg parts: String): String = "Kotlin: " + parts.joinToString(",")
}

suspend fun callJoin(v: AsyncVararg): String = v.join("a", "b", "c")

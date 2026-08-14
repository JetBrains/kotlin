// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: suspend_super_and_inherited.kt

// `notOpen` is final, so it has no vtable slot at all
open class AsyncBase {
    open suspend fun greet(name: String): String = "Kotlin: $name"
    open suspend fun count(): Int = 42
    suspend fun notOpen(): String = "kotlin-final"
}

suspend fun callGreet(base: AsyncBase, name: String): String = base.greet(name)
suspend fun callCount(base: AsyncBase): Int = base.count()
suspend fun callNotOpen(base: AsyncBase): String = base.notOpen()

interface AsyncSpeaker {
    suspend fun speak(): String
}

open class AsyncSpeakerBase : AsyncSpeaker {
    override suspend fun speak(): String = "Kotlin speaks"
}

// Defaulted suspend interface method: a Swift class that inherits a Kotlin class and first-adopts this
// interface, without overriding `describe`, must inherit the Kotlin async default via the non-virtual
// ("_direct") forward async bridge, never recursing through its patched itable slot. The default's
// open self-call to the abstract `tag()` must reach the Swift override.
interface AsyncDefaulter {
    suspend fun tag(): String
    suspend fun describe(): String = "default-describe(" + tag() + ")"
}

suspend fun callAsyncDescribe(d: AsyncDefaulter): String = d.describe()

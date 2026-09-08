// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: suspend_override.kt

// `syncTag` is deliberately NOT suspend
open class AsyncBase {
    open suspend fun greet(name: String): String = "Kotlin: $name"
    open fun syncTag(): String = "kotlin-sync"
}

suspend fun callGreet(base: AsyncBase, name: String): String = base.greet(name)
fun callSyncTag(base: AsyncBase): String = base.syncTag()

interface AsyncSpeaker {
    suspend fun speak(): String
}

open class AsyncSpeakerBase : AsyncSpeaker {
    override suspend fun speak(): String = "Kotlin speaks"
}

suspend fun callSpeak(s: AsyncSpeaker): String = s.speak()

class AsyncPayload(val label: String)

open class AsyncPayloads {
    open suspend fun nothingToReturn() {}
    open suspend fun number(): Int = 1
    open suspend fun payload(): AsyncPayload? = AsyncPayload("kotlin")
}

suspend fun callNothingToReturn(p: AsyncPayloads) = p.nothingToReturn()
suspend fun callNumber(p: AsyncPayloads): Int = p.number()
suspend fun callPayload(p: AsyncPayloads): AsyncPayload? = p.payload()

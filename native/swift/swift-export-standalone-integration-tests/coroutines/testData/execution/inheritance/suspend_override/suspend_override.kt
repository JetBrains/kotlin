// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: suspend_override.kt

open class AsyncBase {
    open suspend fun greet(name: String): String = "Kotlin: $name"
}

suspend fun callGreet(base: AsyncBase, name: String): String = base.greet(name)

interface AsyncSpeaker {
    suspend fun speak(): String
}

open class AsyncSpeakerBase : AsyncSpeaker {
    override suspend fun speak(): String = "Kotlin speaks"
}

suspend fun callSpeak(s: AsyncSpeaker): String = s.speak()

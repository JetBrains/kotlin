// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: suspend_error_propagation.kt

class AsyncException(message: String) : RuntimeException(message)

open class AsyncThrower {
    open suspend fun boom(): String {
        throw AsyncException("kotlin-boom")
    }
}

suspend fun callBoom(t: AsyncThrower): String = t.boom()

interface AsyncSpeaker {
    suspend fun speak(): String
}

open class AsyncSpeakerBase : AsyncSpeaker {
    override suspend fun speak(): String = "Kotlin speaks"
}

suspend fun callSpeak(s: AsyncSpeaker): String = s.speak()

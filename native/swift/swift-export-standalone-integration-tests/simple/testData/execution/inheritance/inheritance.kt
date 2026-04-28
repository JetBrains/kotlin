// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: lib.kt

open class Base {
    open fun greet(): String = "Hello from Kotlin"
}

fun callGreet(base: Base): String = base.greet()

interface Speaker {
    fun speak(): String
    fun volume(): Int
}

open class SpeakerBase : Speaker {
    override fun speak(): String = "Kotlin speaks"
    override fun volume(): Int = 5
}

fun callSpeak(s: Speaker): String = s.speak()
fun callVolume(s: Speaker): Int = s.volume()

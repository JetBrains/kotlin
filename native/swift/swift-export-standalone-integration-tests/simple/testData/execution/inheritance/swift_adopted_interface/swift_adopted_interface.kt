// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: swift_adopted_interface.kt

// The Swift class extends a Kotlin class that does NOT implement the interface, and adopts the interface
// itself. There is no Kotlin member to bind to, so the binding targets the *interface* and the itable slot is
// populated entirely from the Swift side. Regression cover for the implementation-marker gating.

open class Base {
    open fun greet(): String = "Hello from Kotlin"
}

fun callGreet(base: Base): String = base.greet()

interface Speaker {
    fun speak(): String
    fun volume(): Int
}

fun callSpeak(s: Speaker): String = s.speak()
fun callVolume(s: Speaker): Int = s.volume()

// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: inherited_interface_member.kt

// KT-90761. A Kotlin class inherits an interface method without redeclaring it. A Swift subclass of that class must
// reach the inherited Kotlin implementation, and a Swift override of it must win, whether the call comes from Kotlin
// or from Swift.

interface Speaker {
    fun speak(): String = "default"
}

fun speak(s: Speaker): String = s.speak()

open class KotlinSpeakerImpl : Speaker

// A Kotlin subclass overriding the inherited method.
open class KotlinOverridingSpeaker : KotlinSpeakerImpl() {
    override fun speak(): String = "kotlin-override"
}

// The default comes from a super-interface of the implemented one.
interface LoudSpeaker : Speaker

open class LoudSpeakerImpl : LoudSpeaker

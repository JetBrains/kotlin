// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: any_member_override.kt

// When a Kotlin class overrides a member of kotlin.Any, Swift Export exports it as an overridable
// member and installs a reverse bridge into its vtable slot. `kotlin.Any` has its own Obj-C reverse
// adapter for that very slot, so the two used to race -- and `kotlin.Any`, being the least derived
// supertype, was applied last and won. That dropped the Swift override and sent Kotlin dispatch
// into -[KotlinBase description] instead, which re-enters Kotlin and recurses until the stack is
// exhausted.

open class Describable {
    override fun toString(): String = "kotlin-describable"
    override fun hashCode(): Int = 11
}

fun callToString(value: Any): String = value.toString()
fun callHashCode(value: Any): Int = value.hashCode()

// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: any_member_recursion.kt

// A Kotlin class that does not override any member of kotlin.Any still gets `kotlin.Any`'s Obj-C
// reverse adapters installed into the toString/hashCode/equals slots of a Swift subclass's vtable.
// Those adapters dispatch to -description/-hash/-isEqual:, whose KotlinBase implementations call
// back into Kotlin -- so a virtual call from there returned to the adapter and recursed until the
// stack was exhausted (EXC_BAD_ACCESS). Note that kotlin.Any.toString() also calls hashCode()
// virtually, so the `hash` slot has to be handled too or the recursion just moves.

open class Some

open class Greeter {
    open fun greet(): String = "kotlin-greeting"
}

fun callToString(value: Any): String = value.toString()
fun callHashCode(value: Any): Int = value.hashCode()
fun callEquals(lhs: Any, rhs: Any): Boolean = lhs == rhs
fun callGreet(greeter: Greeter): String = greeter.greet()

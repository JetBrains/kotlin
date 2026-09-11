// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: objc_kotlin_base_inheritance.kt

// KT-88251: `KotlinBase.init()` is a plain Objective-C designated initializer, so a class declared in
// Objective-C — with no Swift metadata at all — can inherit `KotlinBase` and take part in the Kotlin
// object graph. Note that such a class cannot implement an exported Kotlin *interface*: the public
// Swift protocol is not `@objc`, and the reverse bridge casts the receiver to it. So this covers the
// class-inheritance and `Any`-bridging side only.

fun echoAny(value: Any): Any = value

fun areSame(lhs: Any, rhs: Any): Boolean = lhs === rhs
fun areEqual(lhs: Any, rhs: Any): Boolean = lhs == rhs
fun hashOf(value: Any): Int = value.hashCode()

class AnyStorage {
    private var stored: Any? = null
    fun store(value: Any) { stored = value }
    fun retrieve(): Any? = stored
}

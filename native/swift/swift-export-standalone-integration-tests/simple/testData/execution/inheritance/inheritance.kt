// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: lib.kt

open class Base {
    open fun greet(): String = "Hello from Kotlin"
}

fun callGreet(base: Base): String = base.greet()

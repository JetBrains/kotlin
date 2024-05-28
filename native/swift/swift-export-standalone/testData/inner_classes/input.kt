// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: main
// FILE: main.kt
class Outer {
    private val bar: Int = 1
    inner class Inner {
        fun foo() = bar
    }
}

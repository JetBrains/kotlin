// KIND: STANDALONE
// MODULE: main
// FILE: main.kt
class Outer {
    private val bar: Int = 1
    inner class Inner {
        fun foo() = bar
    }
}

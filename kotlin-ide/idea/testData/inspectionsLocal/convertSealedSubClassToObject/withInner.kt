// PROBLEM: none

sealed class Sealed

<caret>class SubSealed : Sealed() {
    val x: Int = 42

    inner class Inner {
        fun foo() = x
    }
}
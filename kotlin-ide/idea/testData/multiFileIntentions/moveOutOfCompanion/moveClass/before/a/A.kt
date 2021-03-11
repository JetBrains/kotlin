package a

class A {
    companion object {
        fun foo() {

        }

        class <caret>B {

        }
    }
}

fun foo() {
    A.Companion.B()
}
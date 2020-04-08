class A {
    companion object {
        class <caret>B {

        }
    }
}

fun foo() {
    A.Companion.B()
}
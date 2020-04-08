// PROBLEM: none

class A {
    fun put(x: Int, y: String) {
    }
}

fun foo() {
    A().<caret>put(1, "foo")
}
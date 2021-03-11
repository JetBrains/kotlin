// IS_APPLICABLE: false

class A {
    fun add(x: Int) {
    }
}

fun foo() {
    A().<caret>add(1)
}
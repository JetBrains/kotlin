package test

class Y(private val a: X) {
    fun test() {
        1.foo()
        with(1) { foo() }
        with(A()) { bar() }
    }
}
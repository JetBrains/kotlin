class A {
    fun makeA(): A { return A() }
    fun makeB(): B { return B() }
    class B() {
    }
}
class C {
    fun useAB() {
        val a = A()
        a.makeA()
        a.makeB()
        val b = A.B()
    }
}

fun used(x: Any?) {}

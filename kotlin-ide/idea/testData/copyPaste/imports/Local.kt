package a

fun f() {
    fun g(): Int {
    }

    class A() {
    }

    <selection>val b = A()
    val a = g()</selection>
}
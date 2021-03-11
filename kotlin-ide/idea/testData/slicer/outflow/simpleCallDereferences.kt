// FLOW: OUT
// WITH_DEREFERENCES

class A {
    fun foo() = 1
    val bar = 2
}

fun A.fooExt() = 1
val A.barExt: Int get() = 2

fun test() {
    val <caret>x = A()

    x.foo()
    x.bar
    x.fooExt()
    x.barExt

    val y: A? = x

    y?.foo()
    y?.bar
    y?.fooExt()
    y?.barExt
}
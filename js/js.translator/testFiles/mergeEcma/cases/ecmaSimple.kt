package foo

open class A {
    val a = "OK"
}

class B: A()

fun box(): String {

    return B().a
}
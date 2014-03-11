package foo

open class A() {
    open fun c() = 2
}

class B() : A() {
}

fun B.d() = c() + 3

fun box(): Boolean {
    return B().d() == 5
}

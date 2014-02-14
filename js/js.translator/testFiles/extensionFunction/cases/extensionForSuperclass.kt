package foo

open class A() {
    open fun c() = 2
}

class B() : A() {
    override fun c() = 3
}

fun B.t() = d() + 1

fun A.d() = c() + 3

fun box(): Boolean {
    return A().d() == 5 && B().d() == 6 && B().t() == 7
}

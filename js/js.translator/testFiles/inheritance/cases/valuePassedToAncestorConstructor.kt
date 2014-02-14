package foo

open class C(a: Int) {
    val b = a
}

class D(c: Int) : C(c + 2) {
}

fun box(): Boolean {
    return (D(0).b == 2)
}
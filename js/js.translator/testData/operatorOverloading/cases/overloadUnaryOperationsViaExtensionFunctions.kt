package foo

class A(val c: Int) {
}


operator fun A.inc() = A(5)
operator fun A.dec() = A(10)

fun box(): Boolean {
    var a = A(1)
    return ((++a).c == 5 && (a++).c == 5 && (--a).c == 10 && (a--).c == 10)
}

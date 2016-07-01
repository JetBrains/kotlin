package lib

var global = ""

inline fun baz(x: () -> Int) = A(1).bar(x())

class A(val y: Int) {
    fun bar(x: Int) = x + y
}
package lib

var global = ""

inline fun baz(x: () -> Int) = ((A(1).B(x()) as Any) as A.B).bar()

class A(val y: Int) {
    inner class B(val x: Int) {
        fun bar() = x + y
    }
}
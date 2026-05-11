package test

class B(val x: Int) {
    fun foo(): Int = x
}

class C(val x: Int) {
    fun foo(): Int = x + 100
}

typealias A = B

package test

open class A(val foo: Int)

open class B : A() {
    val /*rename*/bar = 2
}
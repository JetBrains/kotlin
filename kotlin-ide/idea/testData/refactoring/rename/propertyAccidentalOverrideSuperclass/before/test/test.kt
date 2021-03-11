package test

open class A {
    val foo = 1
}

open class B : A() {
    val /*rename*/bar = 2
}
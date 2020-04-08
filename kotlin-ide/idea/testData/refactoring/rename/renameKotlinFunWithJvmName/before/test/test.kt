package test

class A {
    @JvmName("foo")
    fun /*rename*/first() = 1
}

fun test() {
    A().first()
}
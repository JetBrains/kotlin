package test

class A {
    @get:JvmName("foo")
    @set:JvmName("barNew")
    var first = 1
}

fun test() {
    A().first
    A().first = 1
}
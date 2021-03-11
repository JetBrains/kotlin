package test

class A {
    @get:JvmName("fooNew")
    @set:JvmName("bar")
    var first = 1
}

fun test() {
    A().first
    A().first = 1
}
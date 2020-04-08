package test

class A(@get:JvmName("getFooNew") var first: Int = 1)

fun test() {
    A().first
    A().first = 1
}
package test

class A(@set:JvmName("setBar") var first: Int = 1)

fun test() {
    A().first
    A().first = 1
}
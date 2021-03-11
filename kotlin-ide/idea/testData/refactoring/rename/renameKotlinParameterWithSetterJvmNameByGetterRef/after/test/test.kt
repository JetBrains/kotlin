package test

class A(@set:JvmName("setBar") var second: Int = 1)

fun test() {
    A().second
    A().second = 1
}
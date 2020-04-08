package test

class A(val <caret>name: String)

fun bar() {
    println(A(""))
}
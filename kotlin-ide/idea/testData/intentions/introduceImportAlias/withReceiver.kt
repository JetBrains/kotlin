package my.sample

class A

fun A.check() {}

fun test() {
    val a = A()
    a.check<caret>()
    A().check()
}
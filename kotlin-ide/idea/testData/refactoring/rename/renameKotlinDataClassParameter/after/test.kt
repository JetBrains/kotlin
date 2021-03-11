package test

data class A(val fooNew: Int, val bar: String)

fun test(a: A) {
    a.fooNew
    a.component1()
    a.bar
    a.component2()
}
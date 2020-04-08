// PROBLEM: none

fun JavaTest.usage() {
    fun foo(a: () -> Unit) {}

    foo(JavaTest.FunInterface1<caret> { 10 }) // local foo will be used without SAM constructor
}
// WITH_RUNTIME
// PROBLEM: none

class A() {
    fun bar(): String = null!!
}

fun foo(a: A) {
    a.bar().substring<caret>(0, a.bar().length - 5)
}
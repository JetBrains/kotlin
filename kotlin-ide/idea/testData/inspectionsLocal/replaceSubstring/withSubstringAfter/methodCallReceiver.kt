// WITH_RUNTIME
// PROBLEM: none

class A() {
    fun bar(): String = null!!
}

fun foo(a: A) {
    a.bar().substring<caret>(a.bar().indexOf('x'))
}
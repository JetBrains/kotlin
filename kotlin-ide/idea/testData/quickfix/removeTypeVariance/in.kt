// "Remove 'in' variance from 'T'" "true"
// WITH_RUNTIME

class Test<in T> {
    fun foo(t: T) {}
    fun bar(): <caret>T = TODO()
}
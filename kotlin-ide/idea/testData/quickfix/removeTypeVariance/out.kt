// "Remove 'out' variance from 'T'" "true"
// WITH_RUNTIME

class Test<out T> {
    fun foo(t: <caret>T) {}
    fun bar(): T = TODO()
}
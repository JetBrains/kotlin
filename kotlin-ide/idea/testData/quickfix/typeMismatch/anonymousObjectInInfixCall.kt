// "Change parameter 't' type of function 'foo' to 'T'" "true"
interface T

infix fun Int.foo(t: Int) = this

fun foo() {
    1 foo <caret>object: T{}
}
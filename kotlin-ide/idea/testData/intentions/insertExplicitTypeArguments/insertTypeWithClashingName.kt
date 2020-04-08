// IS_APPLICABLE: true
class Array

fun test() {
    <caret>bar(foo(""))
}

fun foo(vararg x: String) = x

fun <T> bar(vararg ts: T) {}
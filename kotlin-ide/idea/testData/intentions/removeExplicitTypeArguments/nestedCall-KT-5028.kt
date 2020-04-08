// IS_APPLICABLE: false

class Some<T>
fun <T> foo(c: Some<T>) {}

fun test() {
    foo(Some<caret><String>())
}
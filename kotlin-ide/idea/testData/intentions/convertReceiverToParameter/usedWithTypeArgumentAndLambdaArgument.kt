fun <T> <caret>Any.foo(callback: () -> Unit) {}

fun bar() {
    "".foo<Int> {}
}
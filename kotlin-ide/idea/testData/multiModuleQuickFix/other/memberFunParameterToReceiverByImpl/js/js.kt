// "Convert parameter to receiver" "true"

actual class Foo {
    actual fun foo(n: Int, <caret>s: String) {

    }
}

fun Foo.testJs() {
    foo(1, "2")
}
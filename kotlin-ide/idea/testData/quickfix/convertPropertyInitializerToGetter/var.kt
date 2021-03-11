// "Convert property initializer to getter" "true"
// WITH_RUNTIME

fun String.foo() = "bar"

interface A {
    var name = <caret>"The quick brown fox jumps over the lazy dog".foo()
}
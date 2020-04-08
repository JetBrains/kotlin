// "Surround with *arrayOf(...)" "true"
// LANGUAGE_VERSION: 1.2

class Foo<T>(vararg val p: T)

fun test() {
    Foo(p = 123<caret>)
}
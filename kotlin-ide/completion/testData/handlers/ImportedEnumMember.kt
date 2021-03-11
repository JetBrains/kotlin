package foo

import foo.Foo.*

enum class Foo {
    AAA
}

fun take(a: Any){}

fun test() {
    take(A<caret>)
}

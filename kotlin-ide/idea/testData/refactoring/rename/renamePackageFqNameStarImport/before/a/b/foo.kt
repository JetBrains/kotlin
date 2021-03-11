package a.b

class Foo

fun Foo.foo(block: Bar.() -> Unit) {
    Bar().block()
}
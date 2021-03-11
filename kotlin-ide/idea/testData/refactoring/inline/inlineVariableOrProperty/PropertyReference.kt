fun main() {
    listOf<Foo>().filter(Foo::bar)
}

private class Foo {
    val bar<caret> get() = baz
    val baz get() = true
}
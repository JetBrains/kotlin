open class <caret>Foo {
    internal fun bar() = ""
}

class Bar : Foo() {
    val x = bar()
}
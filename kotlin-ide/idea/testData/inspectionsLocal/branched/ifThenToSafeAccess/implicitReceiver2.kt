class Foo {
    fun foo() {}
}

fun Foo?.test() {
    <caret>if (this@test != null) {
        foo()
    }
}
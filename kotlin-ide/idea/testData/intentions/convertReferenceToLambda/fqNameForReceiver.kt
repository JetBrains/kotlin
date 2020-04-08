class Foo {
    class Bar {
        fun foo() {}
    }
}

class Bar {
    fun foo() {}
}

fun use() {
    val f: (Foo.Bar) -> Unit = <caret>Foo.Bar::foo
}
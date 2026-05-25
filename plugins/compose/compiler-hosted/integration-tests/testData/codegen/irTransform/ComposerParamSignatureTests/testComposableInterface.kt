interface Foo {
    @Composable fun bar()
}

class FooImpl : Foo {
    @Composable override fun bar() {}
}

fun used(x: Any?) {}

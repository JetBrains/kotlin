abstract class BaseFoo {
    @Composable abstract fun bar()
}

class FooImpl : BaseFoo() {
    @Composable override fun bar() {}
}

fun used(x: Any?) {}

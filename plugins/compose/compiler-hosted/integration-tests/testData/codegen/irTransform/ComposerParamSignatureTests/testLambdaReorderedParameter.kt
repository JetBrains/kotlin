@Composable fun Foo(a: String, b: () -> Unit) { }
@Composable fun Example() {
    Foo(b={}, a="Hello, world!")
}

fun used(x: Any?) {}

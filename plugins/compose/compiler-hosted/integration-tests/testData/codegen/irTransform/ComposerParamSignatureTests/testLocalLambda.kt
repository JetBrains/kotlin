@Composable fun Bar(content: @Composable () -> Unit) {
    val foo = @Composable { x: Int -> print(x)  }
    foo(123)
    content()
}

fun used(x: Any?) {}

@Composable fun Foo(content: @Composable () -> Unit) {
}
@Composable fun Bar() {
    Foo {}
}

fun used(x: Any?) {}

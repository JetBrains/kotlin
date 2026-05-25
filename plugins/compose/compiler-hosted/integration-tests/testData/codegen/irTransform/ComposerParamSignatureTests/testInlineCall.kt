@Composable inline fun Example(content: @Composable () -> Unit) {
    content()
}

@Composable fun Test() {
    Example {}
}

fun used(x: Any?) {}

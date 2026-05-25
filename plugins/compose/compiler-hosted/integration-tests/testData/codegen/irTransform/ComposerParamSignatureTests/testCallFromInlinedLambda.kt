@Composable fun Foo() {
    listOf(1, 2, 3).forEach { Bar(it) }
}

@Composable fun Bar(a: Int) {}

fun used(x: Any?) {}

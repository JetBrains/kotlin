fun interface A {
    @Composable fun compute(value: Int): Unit
}
fun Example(a: A) {
    Example { it -> a.compute(it) }
}

fun used(x: Any?) {}

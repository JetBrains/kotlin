val a = compositionLocalOf { 123 }
@Composable fun Foo() {
    val b = a.current
    print(b)
}

fun used(x: Any?) {}

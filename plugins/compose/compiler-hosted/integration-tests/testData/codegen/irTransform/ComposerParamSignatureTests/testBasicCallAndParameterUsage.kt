@Composable fun Foo(a: Int, b: String) {
    print(a)
    print(b)
    Bar(a, b)
}

@Composable fun Bar(a: Int, b: String) {
    print(a)
    print(b)
}

fun used(x: Any?) {}

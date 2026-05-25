@Composable fun Wrap(content: @Composable (x: Int) -> Unit) {
    content(123)
}
@Composable fun App(x: Int) {
    print(x)
    Wrap { a ->
        print(a)
        print(x)
        Wrap { b ->
            print(x)
            print(a)
            print(b)
        }
    }
}

fun used(x: Any?) {}

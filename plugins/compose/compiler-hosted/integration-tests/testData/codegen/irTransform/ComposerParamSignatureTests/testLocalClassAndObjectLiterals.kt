@Composable
fun Wat() {}

@Composable
fun Foo(x: Int) {
    Wat()
    @Composable fun goo() { Wat() }
    class Bar {
        @Composable fun baz() { Wat() }
    }
    goo()
    Bar().baz()
}

fun used(x: Any?) {}

interface Test {
    @Composable fun Content()
}

class Delegate(val test: Test) : Test by test

fun used(x: Any?) {}

import androidx.compose.runtime.*


interface Test {
    @Composable fun Int.foo(param: Int = remember { 0 })
}

class TestImpl : Test {
    @Composable override fun Int.foo(param: Int) {}
}

@Composable fun CallWithDefaults(test: Test) {
    with(test) {
        42.foo()
        42.foo(0)
    }
}

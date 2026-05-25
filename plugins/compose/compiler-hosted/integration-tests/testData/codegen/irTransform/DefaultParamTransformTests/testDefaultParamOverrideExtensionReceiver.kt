import androidx.compose.runtime.*


interface Test {
    @Composable fun Int.foo(param: Int = remember { 0 })
    @Composable fun Int.bar(param: Int = remember { 0 }): Int = param
}

class TestImpl : Test {
    @Composable override fun Int.foo(param: Int) {}
    @Composable override fun Int.bar(param: Int): Int = 0
}

@Composable fun CallWithDefaults(test: Test) {
    with(test) {
        42.foo()
        42.foo(0)
        42.bar()
        42.bar(0)
    }
}

fun used(x: Any?) {}

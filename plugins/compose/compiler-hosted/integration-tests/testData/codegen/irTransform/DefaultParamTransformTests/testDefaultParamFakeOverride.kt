import androidx.compose.runtime.*


open class Test {
    @Composable open fun foo(param: Int = remember { 0 }) {}
    @Composable open fun bar(param: Int = remember { 0 }): Int = param
}

class TestImpl : Test() {
    @Composable override fun foo(param: Int) {}
}

@Composable fun CallWithDefaults(test: Test) {
    test.foo()
    test.foo(0)
    test.bar()
    test.bar(0)
}

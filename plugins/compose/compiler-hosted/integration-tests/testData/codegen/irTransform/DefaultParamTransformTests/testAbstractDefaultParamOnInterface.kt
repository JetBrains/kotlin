import androidx.compose.runtime.*


interface Test {
    @Composable fun foo(param: Int = remember { 0 })
}

interface TestBetween : Test {
     @Composable fun betweenFoo(param: Int = remember { 0 })
}

class TestImpl : TestBetween {
    @Composable override fun foo(param: Int) {}
    @Composable override fun betweenFoo(param: Int) {}
}

@Composable fun CallWithDefaults(test: Test, testBetween: TestBetween, testImpl: TestImpl) {
    test.foo()
    test.foo(0)

    testBetween.foo()
    testBetween.foo(0)
    testBetween.betweenFoo()
    testBetween.betweenFoo(0)

    testImpl.foo()
    testImpl.foo(0)
    testImpl.betweenFoo()
    testImpl.betweenFoo(0)
}

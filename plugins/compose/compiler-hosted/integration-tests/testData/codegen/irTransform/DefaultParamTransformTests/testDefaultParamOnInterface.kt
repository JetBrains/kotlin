import androidx.compose.runtime.*


interface Test {
    @Composable fun foo(param: Int = remember { 0 })
    @Composable fun bar(param: Int = remember { 0 }): Int = param
}

interface TestBetween : Test {
     @Composable fun betweenFoo(param: Int = remember { 0 })
     @Composable fun betweenFooDefault(param: Int = remember { 0 }) {}
     @Composable fun betweenBar(param: Int = remember { 0 }): Int = param
}

class TestImpl : TestBetween {
    @Composable override fun foo(param: Int) {}
    @Composable override fun bar(param: Int): Int {
        return super.bar(param)
    }
    @Composable override fun betweenFoo(param: Int) {}
}

@Composable fun CallWithDefaults(test: Test, testBetween: TestBetween, testImpl: TestImpl) {
    test.foo()
    test.foo(0)
    test.bar()
    test.bar(0)

    testBetween.foo()
    testBetween.foo(0)
    testBetween.bar()
    testBetween.bar(0)
    testBetween.betweenFoo()
    testBetween.betweenFoo(0)
    testBetween.betweenFooDefault()
    testBetween.betweenFooDefault(0)
    testBetween.betweenBar()
    testBetween.betweenBar(0)

    testImpl.foo()
    testImpl.foo(0)
    testImpl.bar()
    testImpl.bar(0)
    testImpl.betweenFoo()
    testImpl.betweenFoo(0)
    testImpl.betweenFooDefault()
    testImpl.betweenFooDefault(0)
    testImpl.betweenBar()
    testImpl.betweenBar(0)
}

fun used(x: Any?) {}

import androidx.compose.runtime.*


interface Test {
    @Composable fun bar(param: Int = remember { 0 }): Int = param
}

interface TestBetween : Test {
     @Composable fun betweenFooDefault(param: Int = remember { 0 }) {}
     @Composable fun betweenBar(param: Int = remember { 0 }): Int = param
}

class TestImpl : TestBetween {
    @Composable override fun bar(param: Int): Int {
        return super.bar(param)
    }
}

@Composable fun CallWithDefaults(test: Test, testBetween: TestBetween, testImpl: TestImpl) {
    test.bar()
    test.bar(0)

    testBetween.bar()
    testBetween.bar(0)
    testBetween.betweenFooDefault()
    testBetween.betweenFooDefault(0)
    testBetween.betweenBar()
    testBetween.betweenBar(0)

    testImpl.bar()
    testImpl.bar(0)
    testImpl.betweenFooDefault()
    testImpl.betweenFooDefault(0)
    testImpl.betweenBar()
    testImpl.betweenBar(0)
}

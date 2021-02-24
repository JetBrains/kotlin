class TestClass<T : TestClass<T>>

class TestClass2 {
    fun test(testClass: TestClass<*>?) = Unit
}
package konan.test

class A {
    fun test() { println("test") }
}

fun getTestSuites(): Collection<TestSuite> {
    return listOf(
            object: TestClass<A>("A") {
                override fun createInstance() = A()

                init {
                    registerTestCase(BasicTestCase("test", A::test))
                }
            }
    )
}

fun main(args:Array<String>) {
    TestRunner(getTestSuites()).run()
}

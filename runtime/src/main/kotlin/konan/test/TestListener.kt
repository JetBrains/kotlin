package konan.test

interface TestListener {
    fun pass(testCase: TestCase)
    fun fail(testCase: TestCase, e: AssertionError)
    fun error(testCase: TestCase, e: Throwable)
    fun ignore(testCase: TestCase)
}
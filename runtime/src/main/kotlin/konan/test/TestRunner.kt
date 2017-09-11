package konan.test

import kotlin.AssertionError

object TestRunner {

    object SimpleTestListener: TestListener {
        override fun pass(testCase: TestCase) = println("Pass: $testCase")
        override fun fail(testCase: TestCase, e: AssertionError) = println("Fail: $testCase (${e.message})")
        override fun error(testCase: TestCase, e: Throwable) = println("Error: $testCase (${e.message})")
        override fun ignore(testCase: TestCase) = println("Ignore: $testCase")

    }

    private val _suites = mutableListOf<TestSuite>()
    val suites: Collection<TestSuite>  get() = _suites

    fun register(suite: TestSuite) = _suites.add(suite)
    fun register(suites: Iterable<TestSuite>) = _suites.addAll(suites)
    fun register(vararg suites: TestSuite) = _suites.addAll(suites)

    fun run() {
        suites.forEach {
            it.run(SimpleTestListener)
        }
    }
}


package konan.test

import kotlin.AssertionError

object TestRunner {

    object SimpleTestListener: TestListener {
        override fun startTesting(runner: TestRunner) = println("Starting testing\n")
        override fun endTesting(runner: TestRunner) = println("Testing finished\n")

        override fun startSuite(suite: TestSuite) = println("Starting test suite: $suite\n")
        override fun endSuite(suite: TestSuite) = println("Test suite finished: $suite\n")

        override fun pass(testCase: TestCase) = println("Pass: $testCase\n")
        override fun fail(testCase: TestCase, e: AssertionError) = println("Fail: $testCase (Exception: ${e.message})\n")
        override fun error(testCase: TestCase, e: Throwable) = println("Error: $testCase (Exception: ${e.message})\n")
        override fun ignore(testCase: TestCase) = println("Ignore: $testCase\n")
    }

    private val _suites = mutableListOf<TestSuite>()
    val suites: Collection<TestSuite>  get() = _suites

    fun register(suite: TestSuite) = _suites.add(suite)
    fun register(suites: Iterable<TestSuite>) = _suites.addAll(suites)
    fun register(vararg suites: TestSuite) = _suites.addAll(suites)

    fun run() {
        SimpleTestListener.startTesting(this)
        suites.forEach {
            SimpleTestListener.startSuite(it)
            it.run(SimpleTestListener)
            SimpleTestListener.endSuite(it)
        }
        SimpleTestListener.endTesting(this)
    }
}


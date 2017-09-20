package konan.test

interface TestLogger: TestListener {
    fun logTestList(runner: TestRunner)
    fun log(message: String)
}

open class BaseTestLogger: BaseTestListener(), TestLogger {

    protected val TestRunner.totalTests
        get() = filterTests().filter { !it.ignored }.size

    protected val TestRunner.totalSuites
        get() = suites.filter { !it.ignored }.size

    override fun log(message: String) = println(message)
    override fun logTestList(runner: TestRunner) {
        runner.filterTests().groupBy { it.suite }.forEach { (suite, tests) ->
            println("${suite.name}.")
            tests.forEach {
                println("  ${it.name}")
            }
        }
    }
}

open class TestLoggerWithStatistics: BaseTestLogger() {

    protected val statistics = MutableTestStatistics()

    override fun startTesting(runner: TestRunner) = statistics.reset()
    override fun endSuite(suite: TestSuite, timeMillis: Long) = statistics.registerSuite()
    override fun pass(testCase: TestCase, timeMillis: Long) = statistics.registerPass()
    override fun fail(testCase: TestCase, e: Throwable, timeMillis: Long) = statistics.registerFail(testCase)
    override fun ignore(testCase: TestCase) = statistics.registerIgnore()
}

class SilentTestLogger: BaseTestLogger() {
    override fun logTestList(runner: TestRunner) {}
    override fun log(message: String) {}
}

class SimpleTestLogger: BaseTestLogger() {
    override fun startTesting(runner: TestRunner) = println("Starting testing")
    override fun endTesting(runner: TestRunner, timeMillis: Long) = println("Testing finished")

    override fun startSuite(suite: TestSuite) = println("Starting test suite: $suite")
    override fun endSuite(suite: TestSuite, timeMillis: Long) = println("Test suite finished: $suite")
    override fun ignoreSuite(suite: TestSuite) = println("Test suite ignored: $suite")

    override fun start(testCase: TestCase) = println("Starting test case: $testCase")
    override fun pass(testCase: TestCase, timeMillis: Long) = println("Passed: $testCase")
    override fun fail(testCase: TestCase, e: Throwable, timeMillis: Long) {
        println("Failed: $testCase. Exception:")
        e.printStackTrace()
    }
    override fun ignore(testCase: TestCase) = println("Ignore: $testCase")
}



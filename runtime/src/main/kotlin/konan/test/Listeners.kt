package konan.test

val TestRunner.totalTests
    get() = suites.map { it.size }.reduce { sum, size -> sum + size }

val TestRunner.totalSuites
    get() = suites.size

class GTestListener: TestListener, AbstractTestStatistics() {

    private val TestCase.prettyName get() = "${suite.name}.$name"
    private val failedTests_ = mutableListOf<TestCase>()

    private var totalSuites = 0

    override fun startTesting(runner: TestRunner) {
        println("[==========] Running ${runner.totalTests} tests from ${runner.totalSuites} test case.")
        println("[----------] Global test environment set-up.") // TODO: just emulation
    }

    private fun printResults(timeMillis: Long) {
        println("[----------] Global test environment tear-down")
        println("[==========] $total tests from $totalSuites test cases ran. ($timeMillis ms total)")
        println("[  PASSED  ] $passed tests.")
        if (hasFailedTests) {
            println("[  FAILED  ] $failed tests, listed below:")
            failedTests.forEach {
                println("[  FAILED  ] ${it.prettyName}")
            }
            println("\n$failed FAILED TESTS")
        }
        if (ignored != 0) {
            println("YOU HAVE $ignored DISABLED TEST(S)")
        }
    }

    override fun endTesting(runner: TestRunner, timeMillis: Long) = printResults(timeMillis)

    override fun startSuite(suite: TestSuite) = println("[----------] ${suite.size} tests from ${suite.name}")
    override fun endSuite(suite: TestSuite, timeMillis: Long) {
        totalSuites++
        println("[----------] ${suite.size} tests from ${suite.name} ($timeMillis ms total)\n")
    }

    override fun ignoreSuite(suite: TestSuite) {}

    override fun start(testCase: TestCase) = println("[ RUN      ] ${testCase.prettyName}")

    override fun pass(testCase: TestCase, timeMillis: Long) {
        registerPass()
        println("[       OK ] ${testCase.prettyName} ($timeMillis ms)")
    }

    override fun fail(testCase: TestCase, e: Throwable, timeMillis: Long) {
        registerFail()
        failedTests_.add(testCase)
        e.printStackTrace()
        println("[  FAILED  ] ${testCase.prettyName} ($timeMillis ms)")
    }

    override fun ignore(testCase: TestCase) = registerIgnore()

    override val failedTests: Collection<TestCase>
        get() = failedTests_
    override val hasFailedTests: Boolean
        get() = failed != 0
}

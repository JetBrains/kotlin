package konan.test

class GTestLogger : TestLoggerWithStatistics() {

    override fun startTesting(runner: TestRunner) {
        super.startTesting(runner)
        println("[==========] Running ${runner.totalTests} tests from ${runner.totalSuites} test case.")
        // Just hack to deal with the Clion parser. TODO: Remove it after changes in the parser.
        println("[----------] Global test environment set-up.")
    }

    private fun printResults(timeMillis: Long) = with (statistics) {
        println("[----------] Global test environment tear-down") // Just hack to deal with the Clion parser.
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

    override fun finishTesting(runner: TestRunner, timeMillis: Long) = printResults(timeMillis)

    override fun startSuite(suite: TestSuite) = println("[----------] ${suite.size} tests from ${suite.name}")

    override fun finishSuite(suite: TestSuite, timeMillis: Long) {
        super.finishSuite(suite, timeMillis)
        println("[----------] ${suite.size} tests from ${suite.name} ($timeMillis ms total)\n")
    }

    override fun start(testCase: TestCase) = println("[ RUN      ] ${testCase.prettyName}")

    override fun pass(testCase: TestCase, timeMillis: Long) {
        super.pass(testCase, timeMillis)
        println("[       OK ] ${testCase.prettyName} ($timeMillis ms)")
    }

    override fun fail(testCase: TestCase, e: Throwable, timeMillis: Long) {
        super.fail(testCase, e, timeMillis)
        e.printStackTrace()
        println("[  FAILED  ] ${testCase.prettyName} ($timeMillis ms)")
    }
}

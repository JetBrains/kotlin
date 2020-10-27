/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal.test

internal class GTestLogger : TestLoggerWithStatistics() {

    private val Collection<TestSuite>.totalTestsNotIgnored: Int
        get() = asSequence().filter { !it.ignored }.sumBy { it.testCases.values.count { !it.ignored } }

    private val Collection<TestSuite>.totalNotIgnored: Int
        get() = filter { !it.ignored }.size

    override fun startIteration(runner: TestRunner, iteration: Int, suites: Collection<TestSuite>) {
        if (runner.iterations != 1) {
            println("\nRepeating all tests (iteration $iteration) . . .\n")
        }
        super.startIteration(runner, iteration, suites)
        println("[==========] Running ${suites.totalTestsNotIgnored} tests from ${suites.totalNotIgnored} test cases.")
        // Just hack to deal with GTest output parsers.
        println("[----------] Global test environment set-up.")
    }

    private fun printResults(timeMillis: Long) = with (statistics) {
        println("[----------] Global test environment tear-down") // Just hack to deal with GTest output parsers.
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

    override fun finishIteration(runner: TestRunner, iteration: Int, timeMillis: Long) = printResults(timeMillis)

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

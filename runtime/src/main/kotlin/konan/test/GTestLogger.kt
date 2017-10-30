/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package konan.test

class GTestLogger : TestLoggerWithStatistics() {

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

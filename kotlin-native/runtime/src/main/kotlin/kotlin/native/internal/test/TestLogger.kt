/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal.test

import kotlin.experimental.ExperimentalNativeApi

@ExperimentalNativeApi
internal interface TestLogger: TestListener {
    fun logTestList(runner: TestRunner, suites: Collection<TestSuite>)
    fun log(message: String)
}

@ExperimentalNativeApi
internal open class BaseTestLogger: BaseTestListener(), TestLogger {
    override fun log(message: String) = println(message)
    override fun logTestList(runner: TestRunner, suites: Collection<TestSuite>) {
        suites.forEach { suite ->
            println("${suite.name}.")
            suite.testCases.values.forEach {
                println("  ${it.name}")
            }
        }
    }
}

@ExperimentalNativeApi
internal open class TestLoggerWithStatistics: BaseTestLogger() {

    protected val statistics = MutableTestStatistics()

    override fun startTesting(runner: TestRunner) = statistics.reset()
    override fun startIteration(runner: TestRunner, iteration: Int, suites: Collection<TestSuite>) = statistics.reset()

    override fun finishSuite(suite: TestSuite, timeMillis: Long) = statistics.registerSuite()
    override fun ignoreSuite(suite: TestSuite) = statistics.registerIgnore(suite)
    override fun pass(testCase: TestCase, timeMillis: Long) = statistics.registerPass()
    override fun fail(testCase: TestCase, e: Throwable, timeMillis: Long) = statistics.registerFail(testCase)
    override fun ignore(testCase: TestCase) = statistics.registerIgnore(testCase)
}

@ExperimentalNativeApi
internal class SilentTestLogger: BaseTestLogger() {
    override fun logTestList(runner: TestRunner, suites: Collection<TestSuite>) {}
    override fun log(message: String) {}
    override fun fail(testCase: TestCase, e: Throwable, timeMillis: Long) = e.printStackTrace()
}

@ExperimentalNativeApi
internal class SimpleTestLogger: BaseTestLogger() {
    override fun startTesting(runner: TestRunner) = println("Starting testing")
    override fun finishTesting(runner: TestRunner, timeMillis: Long) = println("Testing finished")

    override fun startIteration(runner: TestRunner, iteration: Int, suites: Collection<TestSuite>) =
            println("Starting iteration: $iteration")
    override fun finishIteration(runner: TestRunner, iteration: Int, timeMillis: Long) =
            println("Iteration finished: $iteration")

    override fun startSuite(suite: TestSuite) = println("Starting test suite: $suite")
    override fun finishSuite(suite: TestSuite, timeMillis: Long) = println("Test suite finished: $suite")
    override fun ignoreSuite(suite: TestSuite) = println("Test suite ignored: $suite")

    override fun start(testCase: TestCase) = println("Starting test case: $testCase")
    override fun pass(testCase: TestCase, timeMillis: Long) = println("Passed: $testCase")
    override fun fail(testCase: TestCase, e: Throwable, timeMillis: Long) {
        println("Failed: $testCase. Exception:")
        e.printStackTrace()
    }
    override fun ignore(testCase: TestCase) = println("Ignore: $testCase")
}

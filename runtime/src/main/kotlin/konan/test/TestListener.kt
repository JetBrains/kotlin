package konan.test

import kotlin.AssertionError

interface TestListener {
    fun startTesting(runner: TestRunner)
    fun endTesting(runner: TestRunner)

    fun startSuite(suite: TestSuite)
    fun endSuite(suite: TestSuite)

    fun pass(testCase: TestCase)
    fun fail(testCase: TestCase, e: AssertionError)
    fun error(testCase: TestCase, e: Throwable)
    fun ignore(testCase: TestCase)
}
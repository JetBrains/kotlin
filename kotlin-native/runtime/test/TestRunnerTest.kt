import kotlin.native.internal.test.*
import kotlin.native.internal.test.TestListener
import kotlin.native.internal.test.TestRunner
import kotlin.native.internal.test.TestSettings
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class TestRunnerTest {

    @Test
    fun execute() {
        val logger = object : BaseTestLogger() {
            val log = StringBuilder()
            override fun log(message: String) {
                log.append(message)
            }

            override fun logTestList(suites: Collection<TestSuite>) {
                suites.forEach { suite ->
                    log.append("${suite.name}.")
                    suite.testCases.values.forEach {
                        log.append("  ${it.name}")
                    }
                }
            }

            override fun startTesting(settings: TestSettings) {
                log.append("Starting testing")
            }

            override fun finishTesting(settings: TestSettings, timeMillis: Long) {
                log.append("Testing finished")
            }

            override fun startIteration(settings: TestSettings, iteration: Int, suites: Collection<TestSuite>) {
                log.append("Starting iteration: $iteration")
            }

            override fun finishIteration(settings: TestSettings, iteration: Int, timeMillis: Long) {
                log.append("Iteration finished: $iteration")
            }

            override fun startSuite(suite: TestSuite) {
                log.append("Starting test suite: $suite")
            }

            override fun finishSuite(suite: TestSuite, timeMillis: Long) {
                log.append("Test suite finished: $suite")
            }

            override fun ignoreSuite(suite: TestSuite) {
                log.append("Test suite ignored: $suite")
            }

            override fun start(testCase: TestCase) {
                log.append("Starting test case: $testCase")
            }

            override fun pass(testCase: TestCase, timeMillis: Long) {
                log.append("Passed: $testCase")
            }

            override fun fail(testCase: TestCase, e: Throwable, timeMillis: Long) {
                log.append("Failed: $testCase. Exception:")
                log.append(e.dumpStackTrace())
            }

            override fun ignore(testCase: TestCase) {
                log.append("Ignore: $testCase")
            }

        }

        val testRunner = TestRunner(TestSettings(
                testSuites = listOf(TopLevelSuite("test").also {
                    it.registerTestCase("testWithException", { throw IllegalStateException(cause = IllegalArgumentException()) }, true)
                }
                ),
                listeners = setOf(logger),
                logger = logger,
                runTests = true,
                iterations = 0,
                useExitCode = true
        ))
        assertEquals(1, testRunner.run())
        assertContains(logger.log, "cause by")
        assertTrue(logger.log.contains("IllegalStateException"))
        assertTrue(logger.log.contains("IllegalArgumentException"))
    }

}
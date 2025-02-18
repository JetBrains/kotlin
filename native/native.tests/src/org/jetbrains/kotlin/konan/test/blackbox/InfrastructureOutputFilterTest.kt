/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TCTestOutputFilter
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestOutputFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("infrastructure")
class InfrastructureOutputFilterTest {
    @Test
    fun noFiltering() {
        val randomTestOutput = List(100) { (Char.MIN_VALUE..Char.MAX_VALUE).random() }.joinToString("")

        val (filteredOutput, testReport) = TestOutputFilter.NO_FILTERING.filter(randomTestOutput)

        assertEquals(null, testReport)
        assertTrue(randomTestOutput === filteredOutput)
    }

    @Test
    fun noTCMessages() {
        val testOutput = """
            1
            2
            3
            4
            5
        """.trimIndent()

        val (filteredOutput, testReport) = TCTestOutputFilter.filter(testOutput)
        testReport ?: throw AssertionError("Test report expected")

        assertTrue(testReport.isEmpty())
        assertTrue(testOutput == filteredOutput)
    }

    @Test
    fun mixedTCMessages() {
        val testOutput = """
            1
            ##teamcity[testSuiteStarted name='sample.test.Foo']
            2
            ##teamcity[testStarted name='passed']
            3
            ##teamcity[testFinished name='passed']
            4
            ##teamcity[testStarted name='failed']
            5
            ##teamcity[testFailed name='failed' details='foo: something went wrong!|n']
            6
            ##teamcity[testFinished name='failed']
            7
            ##teamcity[testIgnored name='ignored']
            8
            ##teamcity[testSuiteFinished name='sample.test.Foo']
            9
            ##teamcity[testSuiteStarted name='sample.test.Bar']
            10
            ##teamcity[testSuiteFinished name='sample.test.Bar']
            11
            ##teamcity[testSuiteStarted name='sample.test.Baz']
            12
            ##teamcity[testStarted name='passed']
            13
            ##teamcity[testFinished name='passed']
            14
            ##teamcity[testStarted name='failed']
            15
            ##teamcity[testFailed name='failed' details='baz: something went wrong!|n']
            16
            ##teamcity[testFinished name='failed']
            17
            ##teamcity[testIgnored name='ignored']
            18
            ##teamcity[testSuiteFinished name='sample.test.Baz']
            19
            
        """.trimIndent()

        val (filteredOutput, testReport) = TCTestOutputFilter.filter(testOutput)
        testReport ?: throw AssertionError("Test report expected")

        assertTrue(!testReport.isEmpty())
        assertEquals(
            listOf("sample.test.Foo.passed", "sample.test.Baz.passed"),
            testReport.passedTests.map(TestName::toString)
        )
        assertEquals(
            listOf("sample.test.Foo.failed", "sample.test.Baz.failed"),
            testReport.failedTests.map(TestName::toString)
        )
        assertEquals(
            listOf("sample.test.Foo.ignored", "sample.test.Baz.ignored"),
            testReport.ignoredTests.map(TestName::toString)
        )

        assertEquals(
            """
                |1
                |2
                |3
                |4
                |5
                |foo: something went wrong!
                |6
                |7
                |8
                |9
                |10
                |11
                |12
                |13
                |14
                |15
                |baz: something went wrong!
                |16
                |17
                |18
                |19
                |
            """.trimMargin(),
            filteredOutput
        )
    }

    @Test
    fun interruptedTestTCMessage() {
        val testOutput = """
            1
            ##teamcity[testSuiteStarted name='sample.test.Foo']
            2
            ##teamcity[testStarted name='passed']
            3
            ##teamcity[testFinished name='passed']
            4
            ##teamcity[testStarted name='failed']
            5
            
        """.trimIndent()

        val (filteredOutput, testReport) = TCTestOutputFilter.filter(testOutput)
        testReport ?: throw AssertionError("Test report expected")

        assertTrue(!testReport.isEmpty())
        assertEquals(listOf("sample.test.Foo.passed"), testReport.passedTests.map(TestName::toString))
        assertEquals(listOf("sample.test.Foo.failed"), testReport.failedTests.map(TestName::toString))
        assertTrue(testReport.ignoredTests.isEmpty())

        assertEquals(
            """
                |1
                |2
                |3
                |4
                |5
                |
            """.trimMargin(),
            filteredOutput
        )
    }
}

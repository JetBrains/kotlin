/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal.test

import kotlin.experimental.ExperimentalNativeApi
import kotlin.text.StringBuilder

@ExperimentalNativeApi
internal class TeamCityLogger : BaseTestLogger() {

    private fun String.escapeForTC(): String = StringBuilder(length).apply {
        for(char in this@escapeForTC) {
            append(when(char) {
                '|'  -> "||"
                '\'' -> "|'"
                '\n' -> "|n"
                '\r' -> "|r"
                '['  -> "|["
                ']'  -> "|]"
                else -> char
            })
        }
    }.toString()

    private val TestCase.tcName
        get() = name.escapeForTC()

    private val TestSuite.tcName
        get() = name.escapeForTC()

    private fun finish(testCase: TestCase, durationMs: Long) =
            report("testFinished name='${testCase.tcName}' duration='$durationMs'")

    private fun report(msg: String) = println("##teamcity[$msg]")

    override fun start(testCase: TestCase) = report("testStarted" +
            " name='${testCase.tcName}'" +
            " locationHint='ktest:test://${testCase.suite.tcName}.${testCase.tcName}'")
    override fun startSuite(suite: TestSuite) = report("testSuiteStarted" +
            " name='${suite.tcName}'" +
            " locationHint='ktest:suite://${suite.tcName}'")

    override fun ignoreSuite(suite: TestSuite) {
        startSuite(suite)
        suite.testCases.values.forEach { ignore(it) }
        finishSuite(suite, 0L)
    }

    override fun finishSuite(suite: TestSuite, timeMillis: Long) = report("testSuiteFinished name='${suite.tcName}'")

    override fun pass(testCase: TestCase, timeMillis: Long) = finish(testCase, timeMillis)
    override fun fail(testCase: TestCase, e: Throwable, timeMillis: Long) {
        val stackTrace = e.dumpStackTrace().escapeForTC()
        val message = e.message?.escapeForTC()
        report("testFailed name='${testCase.tcName}' message='$message' details='$stackTrace'")
        finish(testCase, timeMillis)
    }

    override fun ignore(testCase: TestCase) = report("testIgnored name='${testCase.tcName}'")
}
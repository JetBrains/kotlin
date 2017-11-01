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

import kotlin.text.StringBuilder

class TeamCityLogger : BaseTestLogger() {

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
    override fun finishSuite(suite: TestSuite, timeMillis: Long) = report("testSuiteFinished name='${suite.tcName}'")

    override fun pass(testCase: TestCase, timeMillis: Long) = finish(testCase, timeMillis)
    override fun fail(testCase: TestCase, e: Throwable, timeMillis: Long) {
        // TODO: Add 'details=...' command with the stack trace (need to implement stacktrace dumping as a string)
        e.printStackTrace()
        report("testFailed name='${testCase.tcName}' message='${e.message?.escapeForTC()}'")
        finish(testCase, timeMillis)
    }

    override fun ignore(testCase: TestCase) = report("testIgnored name='${testCase.tcName}'")
}
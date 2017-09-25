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

    override fun start(testCase: TestCase) = report("testStarted name='${testCase.tcName}'")
    override fun startSuite(suite: TestSuite) = report("testSuiteStarted name='${suite.tcName}'")
    override fun endSuite(suite: TestSuite, timeMillis: Long) = report("testSuiteFinished name='${suite.tcName}'")

    override fun pass(testCase: TestCase, timeMillis: Long) = finish(testCase, timeMillis)
    override fun fail(testCase: TestCase, e: Throwable, timeMillis: Long) {
        // TODO: Add 'details=...' command with the stack trace (need to implement stacktrace dumping as a string)
        e.printStackTrace()
        report("testFailed name='${testCase.tcName}' message='${e.message?.escapeForTC()}'")
        finish(testCase, timeMillis)
    }

    override fun ignore(testCase: TestCase) = report("testIgnored name='${testCase.tcName}'")
}
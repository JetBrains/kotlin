package org.jetbrains.kotlin

import com.google.gson.annotations.Expose
import java.io.PrintWriter
import java.io.StringWriter
import org.gradle.api.Project

enum class TestStatus {
    PASSED,
    FAILED,
    ERROR,
    SKIPPED
}

data class Statistics(
        @Expose var passed: Int = 0,
        @Expose var failed: Int = 0,
        @Expose var error: Int = 0,
        @Expose var skipped: Int = 0) {


    fun pass(count: Int = 1) { passed += count }

    fun skip(count: Int = 1) { skipped += count }

    fun fail(count: Int = 1) { failed += count }

    fun error(count: Int = 1) { error += count }

    fun add(other: Statistics) {
        passed += other.passed
        failed += other.failed
        error += other.error
        skipped += other.skipped
    }
}

val Statistics.total: Int
    get() = passed + failed + error + skipped

class TestFailedException(msg:String) : RuntimeException(msg)


data class KonanTestGroupReport(@Expose val name: String, val suites: List<KonanTestSuiteReport>)

data class KonanTestSuiteReport(@Expose val name: String, val tests: List<KonanTestCaseReport>)

data class KonanTestCaseReport(@Expose val name: String, @Expose val status: TestStatus, @Expose val comment: String? = null)

class KonanTestSuiteReportEnvironment(val name: String, val project: Project, val statistics: Statistics) {
    private val tc = if (Tc.enabled) TeamCityTestPrinter(project) else null
    val tests = mutableListOf<KonanTestCaseReport>()
    fun executeTest(testName: String, action:() -> Unit) {
        var test: KonanTestCaseReport?
        try {
            tc?.startTest(testName)
            action()
            tc?.passTest(testName)
            statistics.pass()
            test = KonanTestCaseReport(testName, TestStatus.PASSED)
        } catch (e:TestFailedException) {
            tc?.failedTest(testName, e)
            statistics.fail()
            test = KonanTestCaseReport(testName, TestStatus.FAILED, "Exception: ${e.message}. Cause: ${e.cause?.message}")
            project.logger.quiet("test: $testName failed")
        } catch (e:Exception) {
            tc?.errorTest(testName, e)
            statistics.error()
            test = KonanTestCaseReport(testName, TestStatus.ERROR, "Exception: ${e.message}. Cause: ${e.cause?.message}")
            project.logger.quiet("error on test: $testName", e)
        }
        test!!.apply {
            tests += this
            if (status == TestStatus.ERROR || status == TestStatus.FAILED) {
                project.logger.quiet("Command to reproduce: ./gradlew $name -Pfilter=${test.name}\n")
            }
        }
    }

    fun skipTest(name: String) {
        tc?.skipTest(name)
        statistics.skip()
        tests += KonanTestCaseReport(name, TestStatus.SKIPPED)
    }

    fun abort(comment: String, throwable: Throwable, testNames: List<String>) {
        testNames.forEach {
            tc?.startTest(it)
            tc?.errorTest(it, java.lang.Exception(throwable))
            tests += KonanTestCaseReport(it, TestStatus.ERROR, "$comment\n${throwable.toString()}")
        }
        abort(throwable, testNames.size)
    }

    fun abort(throwable: Throwable, count: Int) {
        statistics.error(count)
        project.logger.quiet("suite `$name` aborted with exception", throwable)
    }

    internal operator fun invoke(action: (KonanTestSuiteReportEnvironment) -> Unit) {
        tc?.suiteStart(name)
        action(this)
        tc?.suiteFinish(name)
    }
}

class KonanTestGroupReportEnvironment(val project:Project) {
    val statistics = Statistics()
    val suiteReports = mutableListOf<KonanTestSuiteReport>()
    fun suite(suiteName:String, action:(KonanTestSuiteReportEnvironment)->Unit) {
        val konanTestSuiteEnvironment = KonanTestSuiteReportEnvironment(suiteName, project, statistics)
        konanTestSuiteEnvironment {
            action(it)
        }
        suiteReports += KonanTestSuiteReport(suiteName, konanTestSuiteEnvironment.tests)
    }
}

private class TeamCityTestPrinter(val project:Project) {
    fun suiteStart(name: String) {
        teamcityReport("testSuiteStarted name='$name'")
    }

    fun suiteFinish(name: String) {
        teamcityReport("testSuiteFinished name='$name'")
    }

    fun startTest(name: String) {
        teamcityReport("testStarted name='$name'")
    }

    fun passTest(testName: String) = teamcityFinish(testName)


    fun failedTest(testName: String, testFailedException: TestFailedException) {
        teamcityReport("testFailed type='comparisonFailure' name='$testName' message='${testFailedException.message.toTeamCityFormat()}'")
        teamcityFinish(testName)
    }

    fun errorTest(testName: String, exception: Exception) {
        val writer = StringWriter()
        exception.printStackTrace(PrintWriter(writer))
        val rawString  = writer.toString()

        teamcityReport("testFailed name='$testName' message='${exception.message.toTeamCityFormat()}' " +
                "details='${rawString.toTeamCityFormat()}'")
        teamcityFinish(testName)
    }

    fun skipTest(testName: String) {
        teamcityReport("testIgnored name='$testName'")
        teamcityFinish(testName)
    }

    private fun teamcityFinish(testName:String) {
        teamcityReport("testFinished name='$testName'")
    }

    /**
     * Teamcity require escaping some symbols in pipe manner.
     * https://github.com/GitTools/GitVersion/issues/94
     */
    private fun String?.toTeamCityFormat(): String = this?.let {
        it.replace("\\|", "||")
                .replace("\r", "|r")
                .replace("\n", "|n")
                .replace("'", "|'")
                .replace("\\[", "|[")
                .replace("]", "|]")} ?: "null"
    private fun teamcityReport(msg: String) {
        project.logger.quiet("##teamcity[$msg]")
    }
}
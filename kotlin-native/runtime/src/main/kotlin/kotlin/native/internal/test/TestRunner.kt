/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal.test

import kotlin.experimental.ExperimentalNativeApi
import kotlin.IllegalArgumentException
import kotlin.text.StringBuilder
import kotlin.time.TimeSource
import kotlin.time.measureTime

/**
 * Test execution settings.
 */
@ExperimentalNativeApi
internal data class TestSettings(
        val testSuites: List<TestSuite>,
        val listeners: Set<TestListener>,
        val logger: TestLogger,
        val runTests: Boolean,
        val iterations: Int,
        val useExitCode: Boolean
)

/**
 *  Test processor that initializes the [TestSettings] using the command line options passed in [args].
 *
 *  See the [help] for the description of arguments
 */
@ExperimentalNativeApi
internal class TestProcessor(val suites: List<TestSuite>, val args: Array<String>) {
    /**
     * Processes given arguments to set test settings and filter tests to be run
     *
     * @see TestSettings
     */
    fun process(): TestSettings {
        val listeners = mutableSetOf<TestListener>()
        val filters = mutableListOf<TestFilter>()
        var logger: TestLogger = GTestLogger()
        var runTests = true
        var useExitCode = true
        var iterations = 1

        args.filter {
            it.startsWith("--gtest_") || it.startsWith("--ktest_") || it == "--help" || it == "-h"
        }.forEach {
            val arg = it.split('=')
            when (arg.size) {
                1 -> when (arg[0]) {
                    "--gtest_list_tests",
                    "--ktest_list_tests" -> {
                        logger.logTestList(suites.filterWith(filters))
                        runTests = false
                    }
                    "-h",
                    "--help" -> {
                        logger.log(help)
                        runTests = false
                    }
                    "--ktest_no_exit_code" -> useExitCode = false
                    else -> throw IllegalArgumentException("Unknown option: $it\n$help")
                }
                2 -> {
                    val key = arg[0]
                    val value = arg[1]
                    when (key) {
                        "--ktest_logger" -> logger = loggerFromArg(value)
                        "--gtest_filter",
                        "--ktest_filter" -> filters.add(gTestFilterFromArg(value))
                        "--ktest_regex_filter" -> filters.add(regexFilterFromArg(value, true))
                        "--ktest_negative_regex_filter" -> filters.add(regexFilterFromArg(value, false))
                        "--ktest_gradle_filter" -> filters.add(gradleFilterFromArg(value, true))
                        "--ktest_negative_gradle_filter" -> filters.add(gradleFilterFromArg(value, false))
                        "--ktest_repeat",
                        "--gtest_repeat" ->
                            iterations = value.toIntOrNull() ?: throw IllegalArgumentException("Cannot parse number: $value")
                        else -> throw IllegalArgumentException("Unknown option: $it\n$help")
                    }
                }
                else -> throw IllegalArgumentException("Unknown option: $it\n$help")
            }
        }
        val testSuites = suites.filterWith(filters).toList()

        return TestSettings(testSuites, listeners, logger, runTests, iterations, useExitCode)
    }

    private fun String.substringEscaped(range: IntRange) = this.substring(range).let { if (it.isNotEmpty()) Regex.escape(it) else "" }

    private fun String.fromGTestPatterns(): List<Regex> = splitToSequence(':').map(::fromGTestPattern).toList()

    // must be in sync with `fromGTestPattern(String)` in native/native.tests/tests/org/jetbrains/kotlin/konan/test/blackbox/support/runner/TestRun.kt
    private fun fromGTestPattern(pattern: String): Regex {
        val result = StringBuilder()
        var prevIndex = 0
        pattern.forEachIndexed { index, c ->
            if (c == '*' || c == '?') {
                result.append(pattern.substringEscaped(prevIndex until index))
                prevIndex = index + 1
                result.append(if (c == '*') ".*" else ".")
            }
        }
        result.append(pattern.substringEscaped(prevIndex until pattern.length))
        return result.toString().toRegex()
    }

    // region filters

    private fun gTestFilterFromArg(filter: String): TestFilter {
        if (filter.isEmpty()) {
            throw IllegalArgumentException("Empty filter")
        }
        val filters = filter.split('-')
        if (filters.size > 2) {
            throw IllegalArgumentException("Wrong pattern syntax: $filter.")
        }

        val positivePatterns = filters[0].fromGTestPatterns()
        val negativePatterns = filters.getOrNull(1)?.fromGTestPatterns() ?: emptyList()

        return { testCase ->
            positivePatterns.any { testCase.prettyName.matches(it) } &&
                    negativePatterns.none { testCase.prettyName.matches(it) }
        }
    }

    private fun regexFilterFromArg(filter: String, positive: Boolean = true): TestFilter {
        if (filter.isEmpty()) {
            throw IllegalArgumentException("Empty filter")
        }
        val pattern = filter.toRegex()
        return { testCase ->
            testCase.prettyName.matches(pattern) == positive
        }
    }

    private fun gradleFilterFromArg(filter: String, positive: Boolean = true): TestFilter {
        if (filter.isEmpty()) {
            throw IllegalArgumentException("Empty filter")
        }

        val patterns = filter.split(',').map { pattern ->
            pattern.split('*').joinToString(separator = ".*") { Regex.escape(it) }.toRegex()
        }

        fun TestCase.matches(pattern: Regex) =
            prettyName.matches(pattern) || suite.name.matches(pattern)

        return if (positive) {
            { testCase ->
                patterns.any { testCase.matches(it) }
            }
        } else {
            { testCase ->
                patterns.none { testCase.matches(it) }
            }
        }
    }

    inner class FilteredSuite(val innerSuite: TestSuite, val filters: Collection<TestFilter>) : TestSuite by innerSuite {
        private val TestCase.matchFilters: Boolean
            get() = filters.all { it(this) }

        override val size: Int
            get() = testCases.size

        override val testCases: Map<String, TestCase> = innerSuite.testCases.filter { it.value.matchFilters }
        override fun toString() = innerSuite.toString()
    }

    private fun Collection<TestSuite>.filterWith(filters: Collection<TestFilter>): Collection<TestSuite> =
            map { FilteredSuite(it, filters) }

    // endregion

    private fun loggerFromArg(logger: String): TestLogger = when (logger.uppercase()) {
        "GTEST" -> GTestLogger()
        "TEAMCITY" -> TeamCityLogger()
        "SIMPLE" -> SimpleTestLogger()
        "SILENT" -> SilentTestLogger()
        else -> throw IllegalArgumentException("Unknown logger type. Available types: GTEST, TEAMCITY, SIMPLE")
    }

    private val help: String
        get() = """
            |Available options:
            |--gtest_list_tests
            |--ktest_list_tests                                  - Show all available tests.
            |
            |--gtest_filter=POSTIVE_PATTERNS[-NEGATIVE_PATTERNS]
            |--ktest_filter=POSTIVE_PATTERNS[-NEGATIVE_PATTERNS] - Run only the tests whose name matches one of the
            |                                                      positive patterns but none of the negative patterns.
            |                                                      '?' matches any single character; '*' matches any
            |                                                      substring; ':' separates two patterns.
            |
            |--ktest_regex_filter=PATTERN                        - Run only the tests whose name matches the pattern.
            |                                                      The pattern is a Kotlin regular expression.
            |
            |--ktest_negative_regex_filter=PATTERN               - Run only the tests whose name doesn't match the pattern.
            |                                                      The pattern is a Kotlin regular expression.
            |
            |--ktest_gradle_filter=PATTERNS                      - Run only the tests which matches the at least one of the patterns.
            |                                                      '*' matches any number of characters, ',' separates patterns.
            |                                                      A test matches a pattern if:
            |                                                          - its name matches the pattern or
            |                                                          - its class name matches the pattern.
            |
            |--ktest_negative_gradle_filter=PATTERNS             - Don't run tests if they match at least one of the patterns.
            |                                                      The pattern is the same as for the ktest_gradle_filter option.
            |
            |--gtest_repeat=COUNT
            |--ktest_repeat=COUNT                                - Run the tests repeatedly.
            |                                                      Use a negative count to repeat forever.
            |
            |--ktest_logger=GTEST|TEAMCITY|SIMPLE|SILENT         - Use the specified output format. The default one is GTEST.
            |
            |--ktest_no_exit_code                                - Don't return a non-zero exit code if there are failing tests.
        """.trimMargin()
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
private typealias TestFilter = (TestCase) -> Boolean

/**
 * Test runner.
 *
 * Runs tests for the given [TestSettings].
 */
@OptIn(kotlin.time.ExperimentalTime::class)
@ExperimentalNativeApi
internal class TestRunner(val settings: TestSettings) {
    constructor(suites: List<TestSuite>, args: Array<String>) : this(TestProcessor(suites, args).process())

    var exitCode = 0
        private set

    private inline fun sendToListeners(event: TestListener.() -> Unit) {
        settings.logger.event()
        settings.listeners.forEach(event)
    }

    private fun TestSuite.run() {
        // Do not run @BeforeClass/@AfterClass hooks if all test cases are ignored.
        if (testCases.values.all { it.ignored }) {
            testCases.values.forEach { testCase ->
                sendToListeners { ignore(testCase) }
            }
            return
        }

        // Normal path: run all hooks and execute test cases.
        doBeforeClass()
        testCases.values.forEach { testCase ->
            if (testCase.ignored) {
                sendToListeners { ignore(testCase) }
            } else {
                val startTime = TimeSource.Monotonic.markNow()
                try {
                    sendToListeners { start(testCase) }
                    testCase.run()
                    sendToListeners { pass(testCase, startTime.elapsedNow().inWholeMilliseconds) }
                } catch (e: Throwable) {
                    sendToListeners { fail(testCase, e, startTime.elapsedNow().inWholeMilliseconds) }
                    if (settings.useExitCode) {
                        exitCode = 1
                    }
                }
            }
        }
        doAfterClass()
    }

    private fun runIteration(iteration: Int) {
        val suitesFiltered = settings.testSuites
        sendToListeners { startIteration(settings, iteration, suitesFiltered) }
        val iterationTime = measureTime {
            suitesFiltered.forEach {
                if (it.ignored) {
                    sendToListeners { ignoreSuite(it) }
                } else {
                    // Do not run filtered out suites.
                    if (it.size == 0) {
                        return@forEach
                    }
                    sendToListeners { startSuite(it) }
                    val time = measureTime { it.run() }.inWholeMilliseconds
                    sendToListeners { finishSuite(it, time) }
                }
            }
        }.inWholeMilliseconds
        sendToListeners { finishIteration(settings, iteration, iterationTime) }
    }

    fun run(): Int {
        if (!settings.runTests)
            return 0
        sendToListeners { startTesting(settings) }
        val totalTime = measureTime {
            var i = 1
            while (i <= settings.iterations || settings.iterations < 0) {
                runIteration(i)
                i++
            }
        }.inWholeMilliseconds
        sendToListeners { finishTesting(settings, totalTime) }
        return exitCode
    }
}

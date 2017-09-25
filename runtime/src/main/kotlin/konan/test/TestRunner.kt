package konan.test

import kotlin.IllegalArgumentException
import kotlin.system.getTimeMillis
import kotlin.system.measureTimeMillis
import kotlin.text.StringBuilder

object TestRunner {

    private val suites_ = mutableListOf<TestSuite>()
    val suites: Collection<TestSuite>
        get() = suites_

    var logger: TestLogger = GTestLogger()

    val listeners = mutableSetOf<TestListener>()
    val filters = mutableListOf<(TestCase) -> Boolean>()

    var exitCode = 0
        private set

    private val TestCase.matchFilters: Boolean
        get() = filters.map { it(this) }.all { it }

    private val TestSuite.testCasesFiltered
        get() = testCases.values.filter { it.matchFilters }

    // TODO: We can cache it.
    fun filterTests(): Collection<TestCase> = suites.flatMap { it.testCasesFiltered }

    fun register(suite: TestSuite) = suites_.add(suite)
    fun register(suites: Iterable<TestSuite>) = suites_.addAll(suites)
    fun register(vararg suites: TestSuite) = suites_.addAll(suites)

    // TODO: Support short aliases.
    // TODO: Support several test iterations.
    /**
     *  Initialize the TestRunner using the command line options passed in [args].
     *  Returns: true if tests may be ran, false otherwise (there are unrecognized options or just help).
     *  The following options are available:
     *
     *  --gtest_list_tests
     *  --ktest_list_tests                                  - Show all available tests.
     *
     *  --gtest_filter=POSTIVE_PATTERNS[-NEGATIVE_PATTERNS] - Run only the tests whose name matches one of the
     *  --ktest_filter=POSTIVE_PATTERNS[-NEGATIVE_PATTERNS]   positive patterns but none of the negative patterns.
     *                                                        '?' matches any single character; '*' matches any
     *                                                        substring; ':' separates two patterns.
     *
     *  --ktest_regex_filter=PATTERN                        - Run only the tests whose name matches the pattern.
     *                                                        The pattern is a Kotlin regular expression.
     *
     *  --ktest_negative_regex_filter=PATTERN               - Run only the tests whose name doesn't match the pattern.
     *                                                        The pattern is a Kotlin regular expression.
     *
     *  --ktest_logger=GTEST|TEAMCITY|SIMPLE|SILENT         - Use the specified output format. The default one is GTEST.
     */
    fun useArgs(args: Array<String>): Boolean {
        try {
            return parseArgs(args)
        } catch (e: IllegalArgumentException) {
            logger.log("Error: ${e.message}")
            return false
        }
    }

    private fun parseArgs(args: Array<String>): Boolean {
        var result = true
        args.filter {
            it.startsWith("--gtest_") || it.startsWith("--ktest_") || it == "--help"
        }.forEach {
            val arg = it.split('=')
            when (arg.size) {
                1 -> when (arg[0]) {
                    "--gtest_list_tests",
                    "--ktest_list_tests" -> { logger.logTestList(this); result = false }
                    "--help" -> { logger.log(help); result = false }
                    else -> throw IllegalArgumentException("Unknown option: $it\n$help")
                }
                2 -> {
                    val key = arg[0]
                    val value = arg[1]
                    when (key) {
                        "--ktest_logger" -> setLoggerFromArg(value)
                        "--gtest_filter",
                        "--ktest_filter" -> setGTestFilterFromArg(value)
                        "--ktest_regex_filter"-> setRegexFilterFromArg(value, true)
                        "--ktest_negative_regex_filter" -> setRegexFilterFromArg(value, false)
                        else -> throw IllegalArgumentException("Unknown option: $it\n$help")
                    }
                }
                else -> throw IllegalArgumentException("Unknown option: $it\n$help")
            }
        }
        return result
    }

    private fun String.substringEscaped(range: IntRange) =
            this.substring(range).let { if(it.isNotEmpty()) Regex.escape(it) else "" }

    private fun String.toGTestPatterns() = splitToSequence(':').map { pattern ->
        val result = StringBuilder()
        var prevIndex = 0
        pattern.forEachIndexed { index, c ->
            if (c == '*' || c == '?') {
                result.append(pattern.substringEscaped(prevIndex until index))
                prevIndex = index+1
                result.append( if (c == '*') ".*" else ".")
            }
        }
        result.append(pattern.substringEscaped(prevIndex until pattern.length))
        return@map result.toString().toRegex()
    }.toList()

    private fun setGTestFilterFromArg(filter: String) {
        if (filter.isEmpty()) {
            throw IllegalArgumentException("Empty filter")
        }
        val filters = filter.split('-')
        if (filters.size > 2) {
            throw IllegalArgumentException("Wrong pattern syntax: $filter.")
        }

        val positivePatterns = filters[0].toGTestPatterns()
        val negativePatterns = filters.getOrNull(1)?.toGTestPatterns() ?: emptyList()

        this.filters.add { testCase ->
            positivePatterns.all { testCase.prettyName.matches(it) } &&
            negativePatterns.none { testCase.prettyName.matches(it) }
        }
    }

    private fun setRegexFilterFromArg(filter: String, positive: Boolean = true) {
        if (filter.isEmpty()) {
            throw IllegalArgumentException("Empty filter")
        }
        val pattern = filter.toRegex()
        filters.add { testCase ->
            testCase.prettyName.matches(pattern) == positive
        }
    }

    private fun setLoggerFromArg(logger: String) {
        when(logger.toUpperCase()) {
            "GTEST" -> this.logger = GTestLogger()
            "TEAMCITY" -> this.logger = TeamCityLogger()
            "SIMPLE" -> this.logger = SimpleTestLogger()
            "SILENT" -> this.logger = SilentTestLogger()
            else -> throw IllegalArgumentException("Unknown logger type. Available types: GTEST, TEAMCITY, SIMPLE")
        }
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
            |--ktest_logger=GTEST|TEAMCITY|SIMPLE|SILENT         - Use the specified output format. The default one is GTEST.
        """.trimMargin()

    private inline fun sendToListeners(event: TestListener.() -> Unit) {
        logger.event()
        listeners.forEach(event)
    }

    private fun TestSuite.run() {
        doBeforeClass()
        testCasesFiltered.forEach { testCase ->
            if (testCase.ignored) {
                sendToListeners { ignore(testCase) }
            } else {
                val startTime = getTimeMillis()
                try {
                    sendToListeners { start(testCase) }
                    testCase.run()
                    sendToListeners { pass(testCase, getTimeMillis() - startTime) }
                } catch (e: Throwable) {
                    sendToListeners { fail(testCase, e, getTimeMillis() - startTime) }
                    exitCode = 1
                }
            }
        }
        doAfterClass()
    }

    fun run(args: Array<String>): Int {
        return if (useArgs(args)) {
            run()
        } else {
            0
        }
    }

    fun run(): Int {
        sendToListeners { startTesting(this@TestRunner) }
        val totalTime = measureTimeMillis {
            suites.forEach {
                if (it.ignored) {
                    sendToListeners { ignoreSuite(it) }
                } else {
                    sendToListeners { startSuite(it) }
                    val time = measureTimeMillis { it.run() }
                    sendToListeners { finishSuite(it, time) }
                }
            }
        }
        sendToListeners { finishTesting(this@TestRunner, totalTime) }
        return exitCode
    }
}

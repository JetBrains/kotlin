/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support

import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.ENTRY_POINT
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.EXIT_CODE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.EXPECTED_TIMEOUT_FAILURE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FIR_IDENTICAL
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FREE_CINTEROP_ARGS
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FREE_COMPILER_ARGS
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.INPUT_DATA_FILE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.KIND
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.OUTPUT_DATA_FILE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.OUTPUT_REGEX
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.PROGRAM_ARGS
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.TEST_RUNNER
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.OutputDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.konan.test.blackbox.support.util.LLDBSessionSpec
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.test.services.impl.RegisteredDirectivesParser
import org.junit.jupiter.api.Assertions
import java.io.File

internal object TestDirectives : SimpleDirectivesContainer() {
    val KIND by enumDirective<TestKind>(
        description = """
            Usage: // KIND: [REGULAR, STANDALONE, STANDALONE_NO_TR, STANDALONE_LLDB]
            Declares the kind of the test:

            - REGULAR (the default) - include this test into the shared test binary.
              All tested functions should be annotated with @kotlin.Test.

            - STANDALONE - compile the test to a separate test binary.
              All tested functions should be annotated with @kotlin.Test

            - STANDALONE_NO_TR - compile the test to a separate binary that is supposed to have main entry point.
              The entry point can be customized Note that @kotlin.Test annotations are ignored.

            - STANDALONE_LLDB - compile the test to a separate binary and debug with LLDB.
        """.trimIndent()
    )

    val TEST_RUNNER by enumDirective<TestRunnerType>(
        description = """
            Usage: // TEST_RUNNER: [DEFAULT, WORKER, NO_EXIT]
            Specify test runner type.
            Note that this directive makes sense only in combination with // KIND: REGULAR or // KIND: STANDALONE
        """.trimIndent()
    )

    val ENTRY_POINT by stringDirective(
        description = """
            Specify custom program entry point. The default entry point is `main` function in the root package.
            Note that this directive makes sense only in combination with // KIND: STANDALONE_NO_TR or // KIND: STANDALONE_LLDB
        """.trimIndent()
    )

    val MODULE by stringDirective(
        """
            Usage: // MODULE: name[(dependencies)[(friends)]]
            Describes one module.
        """.trimIndent()
    )

    val FILE by stringDirective(
        description = """
            Usage: // FILE: name.kt
            Declares file with specified name in current module.
        """.trimIndent()
    )

    val OUTPUT_DATA_FILE by stringDirective(
        description = """
            Specify the file which contains the expected program output. When program finishes its execution, the actual output (stdout)
            will be compared to the contents of this file.
        """.trimIndent()
    )

    val OUTPUT_REGEX by stringDirective(
        description = """
            The regex that the expected program output should match to. When program finishes its execution, the actual output (stdout)
            will be checked against this regex.
        """.trimIndent(),
        multiLine = true,
    )

    // TODO: to be supported later
//    val OUTPUT_INCLUDES by stringDirective(
//        description = """
//            The text that the expected program output should contain. When program finishes its execution, it will be checked if
//            the actual output (stdout) contains this text.
//        """.trimIndent()
//    )

    // TODO: to be supported later
//    val OUTPUT_NOT_INCLUDES by stringDirective(
//        description = """
//            The text that the expected program output should NOT contain. When program finishes its execution, it will be checked if
//            the actual output (stdout) does nto contain this text.
//        """.trimIndent()
//    )

    val INPUT_DATA_FILE by stringDirective(
        description = """
            Specify the file which contains the text to be passed to process' input (stdin).
            Note that this directive makes sense only in combination with // KIND: STANDALONE_NO_TR
        """.trimIndent()
    )

    val EXIT_CODE by stringDirective(
        description = """
            Specify the exit code that the test should finish with. Example: // EXIT_CODE: 42
            To indicate any non-zero exit code use // EXIT_CODE: !0
            Note that this directive makes sense only in combination with // KIND: STANDALONE_NO_TR
        """.trimIndent()
    )

    val EXPECTED_TIMEOUT_FAILURE by directive(
        description = "Whether the test is expected to fail on timeout"
    )

    val FREE_COMPILER_ARGS by stringDirective(
        description = "Specify free compiler arguments for Kotlin/Native compiler"
    )

    val FREE_CINTEROP_ARGS by stringDirective(
        description = "Specify free CInterop tool arguments"
    )

    // TODO "MUTED_WHEN" directive should be supported not only in AbstractNativeSimpleTest, but also in other hierarchies
    val MUTED_WHEN by enumDirective<MutedOption>(
        description = """
        Usage: // MUTED_WHEN: [K1, K2]
        In native simple tests, specify the pipeline types to mute the test""".trimIndent(),
    )

    val FIR_IDENTICAL by directive(
        description = "Test behavior should be identical for FIR testing"
    )

    val FILECHECK_STAGE by stringDirective(
        description = "Specify a LLVM stage to dump LLVM IR after, and check it with LLVM FileCheck using its directives in test file"
    )

    val DISABLE_NATIVE by stringDirective(
        description = "Test is not compiled/run with neither K1 nor K2 frontend and marked as disabled(GRAY)."
    )

    val DISABLE_NATIVE_K1 by stringDirective(
        description = "Test is not compiled/run with K1 frontend and marked as disabled(GRAY)."
    )

    val DISABLE_NATIVE_K2 by stringDirective(
        description = "Test is not compiled/run with K2 frontend and marked as disabled(GRAY)."
    )

    val IGNORE_NATIVE by stringDirective(
        """
            Usage: // IGNORE_NATIVE: property1=value1[ && property2=value2][ && property3=value3]
            Declares this test is expected to fail in described run configuration on both K1 and K2 frontends.
        """.trimIndent()
    )

    val IGNORE_NATIVE_K1 by stringDirective(
        """
            Usage: // IGNORE_NATIVE_K1: property1=value1[ && property2=value2][ && property3=value3]
            Declares this test is expected to fail in described run configuration on K1 frontend.
        """.trimIndent()
    )

    val IGNORE_NATIVE_K2 by stringDirective(
        """
            Usage: // IGNORE_NATIVE_K2: property1=value1[ && property2=value2][ && property3=value3]
            Declares this test is expected to fail in described run configuration on K2 frontend.
        """.trimIndent()
    )

    val PROGRAM_ARGS by stringDirective(
        description = """
            Command line arguments to pass to the executable.
            Note that this directive makes sense only in combination with // KIND: STANDALONE_NO_TR
        """.trimIndent()
    )

    val ASSERTIONS_MODE by enumDirective<AssertionsMode>(
        description = """
            Force assertions mode to given value, overriding calculated mode which is derived from cacheMode and optimizationMode.
        """.trimIndent()
    )
}

// mimics class `JVMAssertionsMode`
internal enum class AssertionsMode(val description: String) {
    ALWAYS_ENABLE("always-enable"),
    ALWAYS_DISABLE("always-disable"),
    JVM("jvm"),
    LEGACY("legacy");

    companion object {
        val DEFAULT = LEGACY
        fun fromStringOrNull(string: String?) = entries.find { it.description == string }
        fun fromString(string: String?) = fromStringOrNull(string) ?: DEFAULT
    }
}

internal enum class TestKind {
    REGULAR,
    STANDALONE,
    STANDALONE_NO_TR,
    STANDALONE_LLDB;
}

internal enum class TestRunnerType {
    DEFAULT,
    WORKER,
    NO_EXIT
}

internal enum class MutedOption {
    DEFAULT,
    K1,
    K2
}

internal val CINTEROP_SOURCE_EXTENSIONS = setOf("c", "cpp", "m", "mm")
internal val CINTEROP_DEFINITION_EXTENSIONS = setOf("def", "h")
internal val KNOWN_EXTENSIONS = setOf("kt") + CINTEROP_DEFINITION_EXTENSIONS + CINTEROP_SOURCE_EXTENSIONS

internal class TestCInteropArgs(cinteropArgs: List<String>) : TestCompilerArgs(emptyList(), cinteropArgs) {
    constructor(vararg cinteropArgs: String) : this(cinteropArgs.asList())
}

internal open class TestCompilerArgs(
    val compilerArgs: List<String>,
    val cinteropArgs: List<String> = emptyList(),
    val assertionsMode: AssertionsMode = AssertionsMode.DEFAULT,
) {
    constructor(vararg compilerArgs: String) : this(compilerArgs.asList())

    private val uniqueCompilerArgs = compilerArgs.toSet()
    private val uniqueCinteropArgs = cinteropArgs.toSet()
    override fun hashCode() = (uniqueCompilerArgs + uniqueCinteropArgs).hashCode()
    override fun equals(other: Any?) = (other as? TestCompilerArgs)?.uniqueCompilerArgs == uniqueCompilerArgs &&
            other.uniqueCinteropArgs == uniqueCinteropArgs

    operator fun plus(otherCompilerArgs: TestCompilerArgs): TestCompilerArgs = TestCompilerArgs(
        this.compilerArgs + otherCompilerArgs.compilerArgs,
        this.cinteropArgs + otherCompilerArgs.cinteropArgs,
        if (this.assertionsMode == otherCompilerArgs.assertionsMode) this.assertionsMode
        else fail { "Cannot add ${this.assertionsMode} and ${otherCompilerArgs.assertionsMode}" },
    )

    companion object {
        val EMPTY = TestCompilerArgs(emptyList())

        fun findForbiddenArgs(compilerArgs: Iterable<String>): Set<String> = buildSet {
            addAll(compilerArgs)
            retainAll(EXPLICITLY_FORBIDDEN_COMPILER_ARGS)
            compilerArgs.mapNotNullTo(this) { arg ->
                if (EXPLICITLY_FORBIDDEN_COMPILER_ARG_PREFIXES.any { prefix -> arg.startsWith(prefix) }) arg else null
            }
        }

        /** The set of compiler args that are not permitted to be explicitly specified using [FREE_COMPILER_ARGS]. */
        private val EXPLICITLY_FORBIDDEN_COMPILER_ARGS = setOf(
            "-trn", "-generate-no-exit-test-runner",
            "-tr", "-generate-test-runner",
            "-trw", "-generate-worker-test-runner",
            "-nomain",
            "-output",
            "-entry", "-e",
            "-produce",
            "-repo",
            "-target",
            "-Xinclude",
            "-g", "-opt",
            "-memory-model",
            "-Xcheck-state-at-external-calls"
        )

        /** The set of compiler arg prefixes that are not permitted to be explicitly specified using [FREE_COMPILER_ARGS]. */
        private val EXPLICITLY_FORBIDDEN_COMPILER_ARG_PREFIXES = setOf(
            "-Xgc="
        )
    }
}

internal fun parseTestKind(registeredDirectives: RegisteredDirectives, location: Location): TestKind? {
    if (KIND !in registeredDirectives)
        return null // The default is determined by TEST_KIND global property

    val values = registeredDirectives[KIND]
    return values.singleOrNull() ?: fail { "$location: Exactly one test kind expected in $KIND directive: $values" }
}

internal fun parseTestRunner(registeredDirectives: RegisteredDirectives, location: Location): TestRunnerType {
    if (TEST_RUNNER !in registeredDirectives)
        return TestRunnerType.DEFAULT // The default one.

    val values = registeredDirectives[TEST_RUNNER]
    return values.singleOrNull() ?: fail { "$location: Exactly one test runner type expected in $TEST_RUNNER directive: $values" }
}

internal fun parseEntryPoint(registeredDirectives: RegisteredDirectives, location: Location): String {
    if (ENTRY_POINT !in registeredDirectives)
        return "main" // The default one.

    val values = registeredDirectives[ENTRY_POINT]
    val entryPoint = values.singleOrNull() ?: fail { "$location: Exactly one entry point expected in $ENTRY_POINT directive: $values" }
    assertTrue(entryPoint.isNotEmpty()) { "$location: Invalid entry point in $ENTRY_POINT directive: $entryPoint" }

    return entryPoint
}

internal fun parseLLDBSpec(testDataFile: File, registeredDirectives: RegisteredDirectives, settings: Settings): LLDBSessionSpec {
    val firIdentical = FIR_IDENTICAL in registeredDirectives
    val firSpecificExt = if (settings.get<PipelineType>() == PipelineType.K2 && !firIdentical) "fir." else ""
    val specFilePathWithoutExtension = testDataFile.absolutePath.removeSuffix(testDataFile.extension)
    val specFileLocation = "$specFilePathWithoutExtension${firSpecificExt}txt"
    val specFile = File(specFileLocation)
    return try {
        LLDBSessionSpec.parse(specFile.readText())
    } catch (e: Exception) {
        Assertions.fail<Nothing>("${testDataFile.absolutePath}: Cannot parse LLDB session specification: " + e.message, e)
    }
}

internal fun parseModule(parsedDirective: RegisteredDirectivesParser.ParsedDirective, location: Location): TestModule.Exclusive {
    val module = parsedDirective.values.singleOrNull()?.toString()?.let(TEST_MODULE_REGEX::matchEntire)?.let { match ->
        val name = match.groupValues[1]
        val directDependencySymbols = match.groupValues[3].split(',').filter(String::isNotEmpty).toSet()
        val directFriendSymbols = match.groupValues[5].split(',').filter(String::isNotEmpty).toSet()
        val directDependsOnSymbols = match.groupValues[7].split(',').filter(String::isNotEmpty).toSet()

        val friendsThatAreNotDependencies = directFriendSymbols - directDependencySymbols
        assertTrue(friendsThatAreNotDependencies.isEmpty()) {
            "$location: Found friends that are not dependencies: $friendsThatAreNotDependencies"
        }

        TestModule.Exclusive(name, directDependencySymbols, directFriendSymbols, directDependsOnSymbols)
    }

    return module ?: fail {
        """
            $location: Invalid contents of ${parsedDirective.directive} directive: ${parsedDirective.values}
            ${parsedDirective.directive.description}
        """.trimIndent()
    }
}

private val TEST_MODULE_REGEX = Regex("^([a-zA-Z0-9_]+)(" +          // name
                                              "\\(([a-zA-Z0-9_,]*)\\)" +    // dependencies
                                              "(\\(([a-zA-Z0-9_,]*)\\))?" + // friends
                                              "(\\(([a-zA-Z0-9_,]*)\\))?" + // dependsOn
                                              ")?$")

internal fun parseFileName(parsedDirective: RegisteredDirectivesParser.ParsedDirective, location: Location): String {
    val fileName = parsedDirective.values.singleOrNull()?.toString()
        ?: fail {
            """
                $location: Exactly one file name expected in ${parsedDirective.directive} directive: ${parsedDirective.values}
                ${parsedDirective.directive.description}
            """.trimIndent()
        }

    val fileExtension = fileName.split(".").last()
    if (fileExtension in KNOWN_EXTENSIONS)
        assertTrue(fileName.length > 3 && '/' !in fileName && '\\' !in fileName) {
            "$location: Invalid file name with extension $fileExtension in ${parsedDirective.directive} directive: $fileName"
        }
    else
        assertTrue(false) {
            "$location: Invalid file extension .$fileExtension in ${parsedDirective.directive} directive: $fileName"
        }

    return fileName
}

internal fun parseExpectedTimeoutFailure(registeredDirectives: RegisteredDirectives): Boolean =
    EXPECTED_TIMEOUT_FAILURE in registeredDirectives

internal fun parseExpectedExitCode(registeredDirectives: RegisteredDirectives, location: Location): TestRunCheck.ExitCode {
    if (EXIT_CODE !in registeredDirectives)
        return TestRunCheck.ExitCode.Expected(0)

    val values = registeredDirectives[EXIT_CODE]
    val exitCode = values.singleOrNull()
        ?: fail { "$location: Exactly one exit code expected in $EXIT_CODE directive: $values" }

    return when (exitCode) {
        "!0" -> TestRunCheck.ExitCode.AnyNonZero
        else -> exitCode.toIntOrNull()?.let(TestRunCheck.ExitCode::Expected)
            ?: fail { "$location: Invalid exit code specified in $EXIT_CODE directive: $exitCode" }
    }
}

internal fun parseFreeCompilerArgs(registeredDirectives: RegisteredDirectives, location: Location): TestCompilerArgs {
    val freeCInteropArgs = registeredDirectives[FREE_CINTEROP_ARGS]
    val freeCompilerArgs = registeredDirectives[FREE_COMPILER_ARGS]
    if (freeCompilerArgs.isNotEmpty()) {
        val forbiddenCompilerArgs = TestCompilerArgs.findForbiddenArgs(freeCompilerArgs)
        assertTrue(forbiddenCompilerArgs.isEmpty()) {
            """
            $location: Forbidden compiler arguments found in $FREE_COMPILER_ARGS directive: $forbiddenCompilerArgs
            All arguments: $freeCompilerArgs
        """.trimIndent()
        }
    }
    return TestCompilerArgs(freeCompilerArgs, freeCInteropArgs)
}

internal fun parseOutputDataFile(baseDir: File, registeredDirectives: RegisteredDirectives, location: Location): OutputDataFile? =
    parseFileBasedDirective(baseDir, OUTPUT_DATA_FILE, registeredDirectives, location)?.let { OutputDataFile(file = it) }

internal fun parseInputDataFile(baseDir: File, registeredDirectives: RegisteredDirectives, location: Location): File? =
    parseFileBasedDirective(baseDir, INPUT_DATA_FILE, registeredDirectives, location)

private fun parseFileBasedDirective(
    baseDir: File,
    directive: StringDirective,
    registeredDirectives: RegisteredDirectives,
    location: Location
): File? {
    if (directive !in registeredDirectives)
        return null

    val values = registeredDirectives[directive]
    val file = values.singleOrNull()?.let { baseDir.resolve(it) }
        ?: fail { "$location: Exactly one file expected in $directive directive: $values" }
    assertTrue(file.isFile) { "$location: File specified in $directive directive does not exist or is not a file: $file" }

    return file
}

internal fun parseProgramArguments(registeredDirectives: RegisteredDirectives): List<String> = registeredDirectives[PROGRAM_ARGS]

internal fun parseOutputRegex(registeredDirectives: RegisteredDirectives): TestRunCheck.OutputMatcher? {
    if (OUTPUT_REGEX !in registeredDirectives)
        return null
    val regexStr = registeredDirectives.singleValue(OUTPUT_REGEX)
    val regex = regexStr.toRegex(RegexOption.DOT_MATCHES_ALL)
    return TestRunCheck.OutputMatcher {
        assertTrue(regex.matches(it)) {
            "Regex `$regex` failed to match `$it`"
        }
        true
    }
}

internal class Location(private val testDataFile: File, val lineNumber: Int? = null) {
    override fun toString() = buildString {
        append(testDataFile.path)
        if (lineNumber != null) append(':').append(lineNumber + 1)
    }
}

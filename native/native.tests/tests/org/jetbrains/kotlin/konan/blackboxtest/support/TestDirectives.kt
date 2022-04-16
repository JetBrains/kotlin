/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support

import org.jetbrains.kotlin.konan.blackboxtest.support.TestDirectives.ENTRY_POINT
import org.jetbrains.kotlin.konan.blackboxtest.support.TestDirectives.EXIT_CODE
import org.jetbrains.kotlin.konan.blackboxtest.support.TestDirectives.FREE_COMPILER_ARGS
import org.jetbrains.kotlin.konan.blackboxtest.support.TestDirectives.INPUT_DATA_FILE
import org.jetbrains.kotlin.konan.blackboxtest.support.TestDirectives.KIND
import org.jetbrains.kotlin.konan.blackboxtest.support.TestDirectives.OUTPUT_DATA_FILE
import org.jetbrains.kotlin.konan.blackboxtest.support.TestDirectives.EXPECTED_TIMEOUT_FAILURE
import org.jetbrains.kotlin.konan.blackboxtest.support.TestDirectives.TEST_RUNNER
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunCheck.OutputDataFile
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.test.services.impl.RegisteredDirectivesParser
import java.io.File

internal object TestDirectives : SimpleDirectivesContainer() {
    val KIND by enumDirective<TestKind>(
        description = """
            Usage: // KIND: [REGULAR, STANDALONE, STANDALONE_NO_TR]
            Declares the kind of the test:

            - REGULAR (the default) - include this test into the shared test binary.
              All tested functions should be annotated with @kotlin.Test.

            - STANDALONE - compile the test to a separate test binary.
              All tested functions should be annotated with @kotlin.Test

            - STANDALONE_NO_TR - compile the test to a separate binary that is supposed to have main entry point.
              The entry point can be customized Note that @kotlin.Test annotations are ignored.
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
            Note that this directive makes sense only in combination with // KIND: STANDALONE_NO_TR
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

    // TODO: to be supported later
//    val OUTPUT_REGEX by stringDirective(
//        description = """
//            The regex that the expected program output should match to. When program finishes its execution, the actual output (stdout)
//            will be checked against this regex.
//        """.trimIndent()
//    )

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
}

internal enum class TestKind {
    REGULAR,
    STANDALONE,
    STANDALONE_NO_TR;
}

internal enum class TestRunnerType {
    DEFAULT,
    WORKER,
    NO_EXIT
}

internal class TestCompilerArgs(val compilerArgs: List<String>) {
    private val uniqueCompilerArgs = compilerArgs.toSet()
    override fun hashCode() = uniqueCompilerArgs.hashCode()
    override fun equals(other: Any?) = (other as? TestCompilerArgs)?.uniqueCompilerArgs == uniqueCompilerArgs

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

internal fun parseTestKind(registeredDirectives: RegisteredDirectives, location: Location): TestKind {
    if (KIND !in registeredDirectives)
        return TestKind.REGULAR // The default one.

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

internal fun parseModule(parsedDirective: RegisteredDirectivesParser.ParsedDirective, location: Location): TestModule.Exclusive {
    val module = parsedDirective.values.singleOrNull()?.toString()?.let(TEST_MODULE_REGEX::matchEntire)?.let { match ->
        val name = match.groupValues[1]
        val directDependencySymbols = match.groupValues[3].split(',').filter(String::isNotEmpty).toSet()
        val directFriendSymbols = match.groupValues[5].split(',').filter(String::isNotEmpty).toSet()

        val friendsThatAreNotDependencies = directFriendSymbols - directDependencySymbols
        assertTrue(friendsThatAreNotDependencies.isEmpty()) {
            "$location: Found friends that are not dependencies: $friendsThatAreNotDependencies"
        }

        TestModule.Exclusive(name, directDependencySymbols, directFriendSymbols)
    }

    return module ?: fail {
        """
            $location: Invalid contents of ${parsedDirective.directive} directive: ${parsedDirective.values}
            ${parsedDirective.directive.description}
        """.trimIndent()
    }
}

private val TEST_MODULE_REGEX = Regex("^([a-zA-Z0-9_]+)(\\(([a-zA-Z0-9_,]*)\\)(\\(([a-zA-Z0-9_,]*)\\))?)?$")

internal fun parseFileName(parsedDirective: RegisteredDirectivesParser.ParsedDirective, location: Location): String {
    val fileName = parsedDirective.values.singleOrNull()?.toString()
        ?: fail {
            """
                $location: Exactly one file name expected in ${parsedDirective.directive} directive: ${parsedDirective.values}
                ${parsedDirective.directive.description}
            """.trimIndent()
        }

    assertTrue(fileName.endsWith(".kt") && fileName.length > 3 && '/' !in fileName && '\\' !in fileName) {
        "$location: Invalid file name in ${parsedDirective.directive} directive: $fileName"
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
    if (FREE_COMPILER_ARGS !in registeredDirectives)
        return TestCompilerArgs.EMPTY

    val freeCompilerArgs = registeredDirectives[FREE_COMPILER_ARGS]
    val forbiddenCompilerArgs = TestCompilerArgs.findForbiddenArgs(freeCompilerArgs)
    assertTrue(forbiddenCompilerArgs.isEmpty()) {
        """
            $location: Forbidden compiler arguments found in $FREE_COMPILER_ARGS directive: $forbiddenCompilerArgs
            All arguments: $freeCompilerArgs
        """.trimIndent()
    }

    return TestCompilerArgs(freeCompilerArgs)
}

internal fun parseOutputDataFile(baseDir: File, registeredDirectives: RegisteredDirectives, location: Location): OutputDataFile? =
    parseFileBasedDirective(baseDir, OUTPUT_DATA_FILE, registeredDirectives, location)?.let(TestRunCheck::OutputDataFile)

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

internal class Location(private val testDataFile: File, val lineNumber: Int? = null) {
    override fun toString() = buildString {
        append(testDataFile.path)
        if (lineNumber != null) append(':').append(lineNumber + 1)
    }
}

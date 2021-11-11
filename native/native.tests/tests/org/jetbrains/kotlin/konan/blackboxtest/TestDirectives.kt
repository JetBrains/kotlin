/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.konan.blackboxtest.TestCompilerArgs.Companion.EXPLICITLY_FORBIDDEN_COMPILER_ARGS
import org.jetbrains.kotlin.konan.blackboxtest.TestDirectives.ENTRY_POINT
import org.jetbrains.kotlin.konan.blackboxtest.TestDirectives.FREE_COMPILER_ARGS
import org.jetbrains.kotlin.konan.blackboxtest.TestDirectives.INPUT_DATA_FILE
import org.jetbrains.kotlin.konan.blackboxtest.TestDirectives.KIND
import org.jetbrains.kotlin.konan.blackboxtest.TestDirectives.OUTPUT_DATA_FILE
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
            Specify the file which contains the expected program output. When program finishes its execution the actual output (stdout)
            will be compared to the contents of this file.
        """.trimIndent()
    )

    val INPUT_DATA_FILE by stringDirective(
        description = """
            Specify the file which contains the text to be passed to process' input (stdin).
            Note that this directive makes sense only in combination with // KIND: STANDALONE_NO_TR
        """.trimIndent()
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

internal class TestCompilerArgs(val compilerArgs: List<String>) {
    private val uniqueCompilerArgs = compilerArgs.toSet()
    override fun hashCode() = uniqueCompilerArgs.hashCode()
    override fun equals(other: Any?) = (other as? TestCompilerArgs)?.uniqueCompilerArgs == uniqueCompilerArgs

    companion object {
        val EMPTY = TestCompilerArgs(emptyList())

        /** The set of compiler args that are not permitted to be explicitly specified using [FREE_COMPILER_ARGS]. */
        internal val EXPLICITLY_FORBIDDEN_COMPILER_ARGS = setOf(
            "-trn", "-generate-no-exit-test-runner",
            "-tr", "-generate-test-runner",
            "-trw", "-generate-worker-test-runner",
            "-nomain",
            "-output",
            "-entry",
            "-produce",
            "-repo",
            "-target",
            "-Xinclude"
        )
    }
}

internal fun parseTestKind(registeredDirectives: RegisteredDirectives, location: Location): TestKind {
    if (KIND !in registeredDirectives)
        return TestKind.REGULAR // The default one.

    val values = registeredDirectives[KIND]
    return values.singleOrNull() ?: fail { "$location: Exactly one test kind expected in $KIND directive: $values" }
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

internal fun parseFreeCompilerArgs(registeredDirectives: RegisteredDirectives, location: Location): TestCompilerArgs {
    if (FREE_COMPILER_ARGS !in registeredDirectives)
        return TestCompilerArgs.EMPTY

    val freeCompilerArgs = registeredDirectives[FREE_COMPILER_ARGS]
    val forbiddenCompilerArgs = freeCompilerArgs intersect EXPLICITLY_FORBIDDEN_COMPILER_ARGS
    assertTrue(forbiddenCompilerArgs.isEmpty()) {
        """
            $location: Forbidden compiler arguments found in $FREE_COMPILER_ARGS directive: $forbiddenCompilerArgs
            All arguments: $freeCompilerArgs
        """.trimIndent()
    }

    return TestCompilerArgs(freeCompilerArgs)
}

internal fun parseOutputDataFile(baseDir: File, registeredDirectives: RegisteredDirectives, location: Location): File? =
    parseFileBasedDirective(baseDir, OUTPUT_DATA_FILE, registeredDirectives, location)

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

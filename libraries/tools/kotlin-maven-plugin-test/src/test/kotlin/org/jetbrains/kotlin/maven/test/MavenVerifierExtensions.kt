/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import org.apache.maven.shared.verifier.Verifier
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.util.jar.JarFile
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.sequences.forEach

fun Verifier.printLog() {
    println("====LOG BEGIN====")
    forEachBuildLogLine { println(it) }
    println("====LOG END====")
}

inline fun Verifier.forEachBuildLogLine(action: (String) -> Unit) {
    val logFile = basedir.let(::File).resolve(logFileName)
    logFile.bufferedReader().use { reader ->
        reader.lineSequence().forEach(action)
    }
}

fun Verifier.assertBuildLogContains(substring: String) {
    forEachBuildLogLine { line ->
        if (substring in line) return
    }
    return Assertions.fail("Build log does not contain '$substring'")
}

fun Verifier.assertBuildLogDoesNotContain(vararg substring: String) {
    forEachBuildLogLine { line ->
        for (sub in substring) {
            if (sub in line) {
                Assertions.fail<Unit>("Build log unexpectedly contains '$sub' in line: $line")
            }
        }
    }
}

fun Verifier.assertFileExists(
    relativePath: String,
    messageSupplier: () -> String = { "Expected file not found: $relativePath" },
) {
    val path = Path(basedir).resolve(relativePath)
    assertTrue(path.exists(), messageSupplier)
}

fun Verifier.assertJarExistsAndNotEmpty(relativePath: String) {
    assertFileExists(relativePath)
    val jarFile = Path(basedir).resolve(relativePath).toFile()
    JarFile(jarFile).use { jar ->
        val hasClassEntries = jar.entries().asSequence().any { it.name.endsWith(".class") }
        assertTrue(hasClassEntries) { "JAR $relativePath contains no .class files" }
    }
}

fun Verifier.assertPluginApplied(pluginName: String) {
    assertBuildLogContains("[INFO] Applied plugin: '$pluginName'")
}

fun Verifier.assertSmartDefaultsEnabled() {
    assertBuildLogContains("[INFO] Kotlin smart defaults are enabled")
}

fun Verifier.assertSmartDefaultsNotEnabled() {
    assertBuildLogDoesNotContain("Kotlin smart defaults are enabled")
}

fun Verifier.assertStdlibAutoAdded() {
    assertBuildLogContains("[INFO] Added kotlin-stdlib dependency")
}

fun Verifier.assertStdlibNotAutoAdded() {
    assertBuildLogDoesNotContain("Added kotlin-stdlib dependency")
}

fun Verifier.assertDependencyTreeContains(groupId: String, artifactId: String, version: String, scope: String = "compile") {
    assertBuildLogContains("$groupId:$artifactId:jar:$version:$scope")
}

fun Verifier.assertBuildLogLineCount(substring: String, expectedCount: Int) {
    var actualCount = 0
    forEachBuildLogLine { line ->
        if (substring in line) actualCount++
    }
    Assertions.assertEquals(expectedCount, actualCount) {
        "Expected $expectedCount occurrences of '$substring' in build log, but found $actualCount"
    }
}

fun Verifier.assertTestsPassed(expectedCount: Int) {
    assertBuildLogContains("Tests run: $expectedCount, Failures: 0, Errors: 0, Skipped: 0")
}

fun Verifier.assertScriptGoalDeprecationWarn() {
    assertBuildLogContains("[WARNING] Executing scripts in maven build files is deprecated and will be removed in further release.")
}

fun Verifier.assertCompilationFailed() {
    assertBuildLogContains("Caused by: org.jetbrains.kotlin.maven.KotlinCompilationFailureException: Compilation failure")
}

fun Verifier.assertCompilerArgsContain(vararg expected: String) {
    var foundCompilerArgsLine = false
    forEachBuildLogLine { line ->
        if ("[DEBUG] Kotlin compiler args:" in line) {
            foundCompilerArgsLine = true
            if (expected.all { it in line }) return
        }
    }
    if (!foundCompilerArgsLine) {
        Assertions.fail<Unit>("No '[DEBUG] Kotlin compiler args:' line found in build log. Did you pass -X to the build?")
    }
    Assertions.fail<Unit>("No '[DEBUG] Kotlin compiler args:' line contains all of: ${expected.toList()}")
}

fun Verifier.assertBuildLogContains(vararg substring: String) {
    val substrings = substring.toMutableSet()
    forEachBuildLogLine { line ->
        for (sub in substrings) {
            if (sub in line) {
                substrings.remove(sub)
                if (substrings.isEmpty()) return
                break // we don't expect line to match two or more expected substrings
                // and it is necessary to avoid ConcurrentModificationException as we iterate over substrings
            }
        }
    }

    if (substrings.isEmpty()) return
    Assertions.fail<Unit> {
        buildString {
            appendLine("Build log does not contain the following lines: ")
            substrings.forEach { appendLine("'$it'") }
        }
    }
}

fun Verifier.assertCompiledKotlin(vararg expectedPaths: String) {
    val kotlinCompileIteration = Regex("compile iteration: (.*)$")
    val normalizedActualPaths = mutableSetOf<String>()
    val workingDirPath = File(basedir).toPath().toRealPath().toAbsolutePath()

    forEachBuildLogLine { line ->
        kotlinCompileIteration.find(line)?.let { match ->
            val compiledFiles = match.groupValues[1].split(",")
            for (path in compiledFiles) {
                if (path.isBlank()) continue
                val file = File(path.trim())
                val relativePath = workingDirPath.relativize(file.toPath().toAbsolutePath())
                normalizedActualPaths.add(relativePath.normalize().toString())
            }
        }
    }

    val actualSorted = normalizedActualPaths.sorted().joinToString("\n")
    val expectedSorted = expectedPaths.map { File(it).toPath().normalize().toString() }.sorted().joinToString("\n")

    Assertions.assertEquals(expectedSorted, actualSorted, "Compiled files differ")
}

fun Verifier.assertFilesExist(vararg paths: String) {
    val base = File(basedir)
    for (path in paths) {
        val file = base.resolve(path)
        Assertions.assertTrue(file.exists()) { "$file does not exist" }
    }
}
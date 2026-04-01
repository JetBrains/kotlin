/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import org.apache.maven.shared.verifier.Verifier
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import java.io.File
import java.util.jar.JarFile
import kotlin.io.path.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists
import kotlin.sequences.forEach

fun Verifier.printLog() {
    println("====LOG BEGIN====")
    forEachBuildLogLine { println(it) }
    println("====LOG END====")
}

inline fun Verifier.forEachBuildLogLine(action: (String) -> Unit) {
    val logFile = Path(basedir).resolve(logFileName)
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
    val jarPath = Path(basedir).resolve(relativePath)
    JarFile(jarPath.toFile()).use { jar ->
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

fun Verifier.assertZipContains(relativePath: String, entryName: String) {
    assertFileExists(relativePath)
    val zipPath = Path(basedir).resolve(relativePath)
    java.util.zip.ZipFile(zipPath.toFile()).use { zip ->
        assertTrue(zip.getEntry(entryName) != null) { "ZIP $relativePath does not contain entry '$entryName'" }
    }
}

fun Verifier.assertFilesExist(vararg paths: String) {
    val base = File(basedir)
    for (path in paths) {
        val file = base.resolve(path)
        assertTrue(file.exists()) { "$file does not exist" }
    }
}

/**
 * Asserts that a goal matching [executedFirst] appears before a goal matching [executedSecond]
 * in the Maven build plan logged with `-X` (debug mode).
 *
 * Debug mode logs `[DEBUG] Goal:` lines in execution order, e.g.:
 * ```
 * [DEBUG] Goal:          org.jetbrains.kotlin:kotlin-maven-plugin:VERSION:compile (default-compile)
 * [DEBUG] Goal:          org.apache.maven.plugins:maven-compiler-plugin:VERSION:compile (default-compile)
 * ```
 *
 * Use [goalFirst] and [goalSecond] to additionally filter each goal by its name,
 * e.g. `":compile"` or `":test-compile"`. This is useful when different plugins
 * use different goal names for the same phase (e.g. `:test-compile` vs `:testCompile`).
 *
 */
fun Verifier.assertGoalOrderInBuildPlan(
    executedFirst: String,
    executedSecond: String,
    goalFirst: String? = null,
    goalSecond: String? = null,
) {
    val allGoalLines = mutableListOf<String>()
    forEachBuildLogLine { line ->
        if ("[DEBUG] Goal:" in line) {
            allGoalLines.add(line.substringAfter("[DEBUG] Goal:").trim())
        }
    }

    assertTrue(allGoalLines.isNotEmpty()) {
        "No '[DEBUG] Goal:' lines found in build log. Was the build run with -X?"
    }

    val firstIndex = allGoalLines.indexOfFirst { executedFirst in it && (goalFirst == null || goalFirst in it) }
    val secondIndex = allGoalLines.indexOfFirst { executedSecond in it && (goalSecond == null || goalSecond in it) }

    assertTrue(firstIndex >= 0) {
        goalNotFoundMessage(executedFirst, goalFirst, allGoalLines)
    }
    assertTrue(secondIndex >= 0) {
        goalNotFoundMessage(executedSecond, goalSecond, allGoalLines)
    }
    assertTrue(firstIndex < secondIndex) {
        "'$executedFirst' (position $firstIndex) was expected before '$executedSecond' (position $secondIndex) in build plan.\n" +
                "Actual order:\n${allGoalLines.joinToString("\n")}"
    }
}

private fun goalNotFoundMessage(plugin: String, goalFilter: String?, allGoalLines: List<String>): String {
    val pluginLines = allGoalLines.filter { plugin in it }
    return if (pluginLines.isNotEmpty() && goalFilter != null) {
        "Plugin '$plugin' found in build plan but no entry matches goal filter '$goalFilter'.\n" +
                "Goals for '$plugin':\n${pluginLines.joinToString("\n")}"
    } else {
        "No build plan entry matching plugin '$plugin'${goalFilter?.let { " and goal filter '$it'" } ?: ""} found.\n" +
                "All entries in build plan:\n${allGoalLines.joinToString("\n")}"
    }
}

fun Verifier.assertClassFileMajorVersion(relativePath: String, expectedMajorVersion: Int) {
    val classFile = File(basedir).resolve(relativePath)
    assertTrue(classFile.exists()) { "Class file not found: $classFile" }
    var majorVersion = -1
    ClassReader(classFile.readBytes()).accept(object : ClassVisitor(Opcodes.API_VERSION) {
        override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<String>?) {
            majorVersion = version and 0xFFFF  // low 16 bits = major version
        }
    }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
    assertEquals(expectedMajorVersion, majorVersion) {
        "Expected major version $expectedMajorVersion but got $majorVersion for $relativePath"
    }
}

fun Verifier.assertClassFinal(className: String, isFinal: Boolean) {
    val classFile = File(basedir).resolve("target/classes/${className.replace('.', '/')}.class")
    assertTrue(classFile.exists()) { "Class file not found: $classFile" }
    var accessFlags = 0
    ClassReader(classFile.readBytes()).accept(object : ClassVisitor(Opcodes.API_VERSION) {
        override fun visit(
            version: Int, access: Int, name: String,
            signature: String?, superName: String?, interfaces: Array<String>?,
        ) {
            accessFlags = access
        }
    }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
    if (isFinal) {
        assertTrue(accessFlags and Opcodes.ACC_FINAL != 0) { "Expected $className to be final" }
    } else {
        assertFalse(accessFlags and Opcodes.ACC_FINAL != 0) { "Expected $className to be open (not final)" }
    }
}
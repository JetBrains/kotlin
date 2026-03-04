/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import org.apache.maven.shared.verifier.Verifier
import org.junit.jupiter.api.Assertions
import java.io.File
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
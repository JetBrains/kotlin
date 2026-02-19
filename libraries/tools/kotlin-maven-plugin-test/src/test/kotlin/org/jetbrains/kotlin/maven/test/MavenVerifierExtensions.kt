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
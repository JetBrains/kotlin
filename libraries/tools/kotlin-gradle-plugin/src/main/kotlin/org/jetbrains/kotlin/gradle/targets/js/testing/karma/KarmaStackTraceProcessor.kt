/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

private const val WEBPACK_PROTOCOL = "webpack://"
private const val KARMA_SOURCE_MAP_DELIMITER = " <-"

private const val STACK_TRACE_DELIMITER = "at "
private const val WEBPACK_LOCAL_DELIMITER = ".."

fun processKarmaStackTrace(stackTrace: String): String {
    return stackTrace.lines()
        .map(::processWebpackName)
        .joinToString("\n") { line ->
            val index = line.indexOf(KARMA_SOURCE_MAP_DELIMITER)
            if (index == -1)
                line
            else
                line
                    .removeRange(index, line.length - 1)
                    .replace(WEBPACK_PROTOCOL, "")
        }
}

fun processWebpackName(line: String): String {
    // example: "at MyTest../kotlin/check-js-test-test.js.MyTest.foo (/src/test/kotlin/MyTest.kt:7:8)"
    // should be "at MyTest.foo (/src/test/kotlin/MyTest.kt:7:8)"
    val stackTraceDelimiter = line.indexOf(STACK_TRACE_DELIMITER)
    val webpackLocalDelimiter = line.indexOf(WEBPACK_LOCAL_DELIMITER)
    if (stackTraceDelimiter == -1 || webpackLocalDelimiter == -1) {
        return line
    }

    val traceStartIndex = stackTraceDelimiter + STACK_TRACE_DELIMITER.length
    val name = line.substring(
        traceStartIndex,
        webpackLocalDelimiter
    ) // MyTest
    val fileStart = line.indexOf("(")
    val fullJsName = line.substring(webpackLocalDelimiter, fileStart) // ../kotlin/check-js-test-test.js.MyTest.foo

    val nameIndex = fullJsName.indexOf(name)
    if (nameIndex == -1) {
        return line
    }

    return line.replaceRange(traceStartIndex, fileStart, fullJsName.substring(nameIndex))
}
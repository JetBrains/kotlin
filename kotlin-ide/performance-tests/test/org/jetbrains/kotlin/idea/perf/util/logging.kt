/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.util

import java.io.PrintWriter
import java.io.StringWriter

inline fun gradleMessage(block: () -> String) {
    print("#gradle ${block()}")
}

inline fun logMessage(message: () -> String) {
    println("-- ${message()}")
}

fun logMessage(t: Throwable, message: () -> String) {
    val writer = StringWriter()
    PrintWriter(writer).use {
        t.printStackTrace(it)
    }
    println("-- ${message()}:\n$writer")
}

object TeamCity {
    inline fun message(block: () -> String) {
        println("##teamcity[${block()}]")
    }

    fun suite(name: String, block: () -> Unit) {
        message { "testSuiteStarted name='$name'" }
        try {
            block()
        } finally {
            message { "testSuiteFinished name='$name'" }
        }
    }

    fun test(name: String, durationMs: Long? = null, errors: List<Throwable>, block: () -> Unit) {
        test(name, durationMs, errorDetails = if (errors.isNotEmpty()) toDetails(errors) else null, block = block)
    }

    fun test(name: String?, durationMs: Long? = null, includeStats: Boolean = true, errorDetails: String? = null, block: () -> Unit) {
        name?.let { testStarted(it) }
        try {
            block()
        } finally {
            name?.let {
                if (includeStats) statValue(it, durationMs ?: -1)
                if (errorDetails != null) {
                    testFailed(it, errorDetails)
                } else {
                    testFinished(it, durationMs)
                }
            }
        }
    }

    inline fun statValue(name: String, value: Any) {
        message { "buildStatisticValue key='$name' value='$value'" }
    }

    inline fun testStarted(testName: String) {
        message { "testStarted name='$testName' captureStandardOutput='true'" }
    }

    inline fun metadata(testName: String, name: String, value: Number) {
        message { "testMetadata testName='$testName' name='$name' type='number' value='$value'" }
    }

    inline fun artifact(testName: String, name: String, artifactPath: String) {
        message { "testMetadata testName='$testName' name='$name' type='artifact' value='$artifactPath'" }
    }

    inline fun testFinished(testName: String, durationMs: Long? = null) {
        message { "testFinished name='$testName'${durationMs?.let { " duration='$durationMs'" } ?: ""}" }
    }

    fun testFailed(testName: String, error: Throwable) = testFailed(testName, toDetails(listOf(error))!!)

    fun testFailed(testName: String, details: String) {
        message { "testFailed name='$testName' message='Exceptions reported' details='${escape(details)}'" }
    }

    private fun escape(s: String) = s.replace("|", "||")
        .replace("[", "|[")
        .replace("]", "|]")
        .replace("\r", "|r")
        .replace("\n", "|n")
        .replace("'", "|'")

    private fun toDetails(errors: List<Throwable>): String? {
        if (errors.isEmpty()) return null
        val detailsWriter = StringWriter()
        val errorDetailsPrintWriter = PrintWriter(detailsWriter)
        errors.forEach {
            it.printStackTrace(errorDetailsPrintWriter)
            errorDetailsPrintWriter.println()
        }
        errorDetailsPrintWriter.close()
        return detailsWriter.toString()
    }
}

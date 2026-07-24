/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.logging.LogLevel
import org.gradle.internal.logging.events.OutputEvent
import org.gradle.internal.logging.events.OutputEventListener
import org.gradle.internal.logging.slf4j.OutputEventListenerBackedLoggerContext
import org.slf4j.LoggerFactory
import kotlin.test.fail

private class KotlinFunctionalTestLogEventListener(
    private val loggerContext: OutputEventListenerBackedLoggerContext,
) : OutputEventListener {
    private var suppressOriginalLogger: Boolean = false
    private val originalListener: OutputEventListener = loggerContext.outputEventListener

    private var collector: MutableList<OutputEvent>? = null

    override fun onOutput(event: OutputEvent) {
        collector?.add(event)
        if (!suppressOriginalLogger) originalListener.onOutput(event)
    }

    /**
     * Calls [code] and listens to all events it emits in [onOutput], when [code] finishes returns them back.
     */
    @Synchronized
    fun <T> withLogCapture(
        level: LogLevel,
        suppressOriginalLogger: Boolean,
        code: () -> T,
    ): Pair<T, List<OutputEvent>> {
        val collector = mutableListOf<OutputEvent>()
        this.collector = collector

        val suppressOriginalLoggerOld = this.suppressOriginalLogger
        this.suppressOriginalLogger = suppressOriginalLogger
        if (suppressOriginalLogger && !suppressOriginalLoggerOld) {
            println("ℹ️Global Gradle logger was muted for KGP Functional test to intercept its logs")
        }

        try {
            val res = withNewLogLevel(level) { code() }
            return res to collector
        } finally {
            this.collector = null
            this.suppressOriginalLogger = suppressOriginalLoggerOld
        }
    }

    fun <T> withNewLogLevel(logLevel: LogLevel, code: () -> T): T = synchronized(loggerContext) {
        val originalLevel = loggerContext.level
        loggerContext.level = logLevel
        try {
            code()
        } finally {
            loggerContext.level = originalLevel
        }
    }
}

private lateinit var eventListener: KotlinFunctionalTestLogEventListener
private val initializedEventListener: KotlinFunctionalTestLogEventListener
    get() {
        if (::eventListener.isInitialized) return eventListener

        val loggerFactory = LoggerFactory.getILoggerFactory()
        check(loggerFactory is OutputEventListenerBackedLoggerContext) {
            "Expected OutputEventListenerBackedLoggerContext, but got ${loggerFactory::class.qualifiedName}"
        }

        synchronized(loggerFactory) {
            if (::eventListener.isInitialized) return eventListener
            eventListener = KotlinFunctionalTestLogEventListener(loggerFactory)
            loggerFactory.outputEventListener = eventListener
            return eventListener
        }
    }

/**
 * Use this method inside a Gradle functional test before or after project evaluation
 * to capture Gradle project logs for further assertions.
 *
 * This API hijacks the current classloader's Log4j instance provided by Gradle to intercept logs.
 * Use with caution.
 */
fun withGradleLogCapture(
    level: LogLevel = LogLevel.DEBUG,
    suppressOriginalLogger: Boolean = true,
    code: () -> Unit,
): List<OutputEvent> {
    val (_, logs) = initializedEventListener.withLogCapture(level, suppressOriginalLogger, code)
    return logs
}

fun <T> withGradleLogCaptureAndResult(
    level: LogLevel = LogLevel.DEBUG,
    suppressOriginalLogger: Boolean = true,
    code: () -> T,
): Pair<T, List<OutputEvent>> =
    initializedEventListener.withLogCapture(level, suppressOriginalLogger, code)

fun List<OutputEvent>.assertLogContains(expected: String, exactMatches: Int = 0) {
    val count = count { it.toString().contains(expected) }

    if (exactMatches > 0) {
        if (count == exactMatches) return
        val allLogs = joinToString("\n")
        fail("Expected log to contain '$expected' exactly $exactMatches times, but found $count occurrences. Actual log:\n$allLogs")
    } else {
        if (count > 0) return
        val allLogs = joinToString("\n")
        fail("Expected log to contain '$expected', actual log:\n$allLogs")
    }
}

fun List<OutputEvent>.assertLogContains(pattern: Regex, exactMatches: Int = 0) {
    val count = count { pattern.containsMatchIn(it.toString()) }

    if (exactMatches > 0) {
        if (count == exactMatches) return
        val allLogs = joinToString("\n")
        fail("Expected log to contain pattern '$pattern' exactly $exactMatches times, but found $count occurrences. Actual log:\n$allLogs")
    } else {
        if (count > 0) return
        val allLogs = joinToString("\n")
        fail("Expected log to contain pattern '$pattern', actual log:\n$allLogs")
    }
}

fun List<OutputEvent>.assertLogDoesNotContain(expected: String) {
    val contains = any { it.toString().contains(expected) }
    if (!contains) return
    val allLogs = joinToString("\n")
    fail("Expected log to not contain '$expected', actual log:\n$allLogs")
}

fun List<OutputEvent>.assertLogDoesNotContain(pattern: Regex) {
    val contains = any { pattern.containsMatchIn(it.toString()) }
    if (!contains) return
    val allLogs = joinToString("\n")
    fail("Expected log to not contain pattern '$pattern', actual log:\n$allLogs")
}

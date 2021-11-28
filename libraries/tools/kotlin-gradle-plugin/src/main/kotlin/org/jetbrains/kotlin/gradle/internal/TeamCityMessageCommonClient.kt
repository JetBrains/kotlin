/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import jetbrains.buildServer.messages.serviceMessages.Message
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageParserCallback
import org.gradle.api.logging.Logger
import org.gradle.internal.logging.progress.ProgressLogger
import org.jetbrains.kotlin.gradle.utils.clearAnsiColor
import java.text.ParseException

class TeamCityMessageCommonClient(
    internal val clientType: LogType,
    internal val log: Logger
) : ServiceMessageParserCallback {

    var afterMessage = false

    var progressLogger: ProgressLogger? = null

    private val errors = mutableListOf<String>()

    private val stackTraceProcessor =
        TeamCityMessageStackTraceProcessor()

    override fun parseException(e: ParseException, text: String) {
        log.error("Failed to parse test process messages: \"$text\"", e)
    }

    override fun serviceMessage(message: ServiceMessage) {
        when (message) {
            is Message -> printMessage(
                message.text,
                LogType.byValueOrNull(
                    message.attributes["type"]
                )
            )
        }

        afterMessage = true
    }

    internal fun testFailedMessage(): String? {
        return if (errors.isNotEmpty())
            errors
                .joinToString("\n")
        else
            null
    }

    private fun printMessage(text: String, type: LogType?) {
        val value = text.trimEnd()
        progressLogger?.progress(value)

        val actualText = if (afterMessage)
            when {
                value.startsWith("\r\n") -> value.removePrefix("\r\n")
                else -> value.removePrefix("\n")
            }
        else value

        val inStackTrace = stackTraceProcessor.process(actualText) { line, logType ->
            log.processLogMessage(line, logType)
            errors.add(line.clearAnsiColor())
        }

        if (inStackTrace) return

        if (type?.isErrorLike() == true) {
            errors.add(actualText.clearAnsiColor())
        }

        type?.let { log.processLogMessage(actualText, it) }
    }

    override fun regularText(text: String) {
        if (clientType == LogType.ERROR || clientType == LogType.WARN) {
            printMessage(text, clientType)
        } else {
            printMessage(text, LogType.DEBUG)
        }
    }
}
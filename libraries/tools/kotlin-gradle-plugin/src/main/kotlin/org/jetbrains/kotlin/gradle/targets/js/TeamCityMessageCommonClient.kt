/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import jetbrains.buildServer.messages.serviceMessages.Message
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageParserCallback
import org.gradle.api.logging.Logger
import org.gradle.internal.logging.progress.ProgressLogger
import java.text.ParseException

class TeamCityMessageCommonClient(
    private val log: Logger,
    private val progressLogger: ProgressLogger
) : ServiceMessageParserCallback {

    private val stackTraceProcessor = TeamCityMessageStackTraceProcessor()

    override fun parseException(e: ParseException, text: String) {
        log.error("Failed to parse test process messages: \"$text\"", e)
    }

    override fun serviceMessage(message: ServiceMessage) {
        when (message) {
            is Message -> printMessage(message.text, message.attributes["type"])
        }
    }

    private fun printMessage(text: String, type: String?) {
        val value = text.trimEnd()
        progressLogger.progress(value)

        var actualType = type
        stackTraceProcessor.process(text) { line ->
            actualType = ERROR
            if (line != null) {
                printMessage(line, actualType)
            }
        }

        actualType?.let { log.processLogMessage(value, it) }
    }

    override fun regularText(text: String) {
        printMessage(text, null)
    }
}
/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.util

import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.LoggingLevel

/**
 * Prints messages to the console according to the passed `consoleMinLevel` parameter,
 * and collects **ALL** log messages, regarding of the set logging level.
 *
 * Useful if you need to verify that user-friendly log messages were emitted,
 * in case they outline actionable problems or help solve a problem and are
 * considered to be a vital part of this product.
 *
 * The collected messages can be retrieved by invoking [collectedLogMessages].
 */
class CollectingDokkaConsoleLogger(
    consoleMinLoggingLevel: LoggingLevel = LoggingLevel.INFO
) : DokkaLogger {

    private val consoleLogger = DokkaConsoleLogger(consoleMinLoggingLevel)
    private val _collectedLogMessages = mutableListOf<String>()

    val collectedLogMessages: List<String> = _collectedLogMessages

    override var warningsCount: Int
        get() = consoleLogger.warningsCount
        set(value) { consoleLogger.warningsCount = value }

    override var errorsCount: Int
        get() = consoleLogger.errorsCount
        set(value) { consoleLogger.errorsCount = value }


    override fun debug(message: String) {
        _collectedLogMessages.add(message)
        consoleLogger.debug(message)
    }

    override fun info(message: String) {
        _collectedLogMessages.add(message)
        consoleLogger.info(message)
    }

    override fun progress(message: String) {
        _collectedLogMessages.add(message)
        consoleLogger.progress(message)
    }

    override fun warn(message: String) {
        _collectedLogMessages.add(message)
        consoleLogger.warn(message)
    }

    override fun error(message: String) {
        _collectedLogMessages.add(message)
        consoleLogger.error(message)
    }
}

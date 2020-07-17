/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.jetbrains.kotlin.gradle.utils.clearAnsiColor
import org.gradle.api.logging.Logger as GradleLogger
import org.slf4j.Logger as SlfLogger

internal fun SlfLogger.processLogMessage(
    message: String,
    type: LogType
) {
    processLogMessageInternal(
        message = message,
        type = type,
        error = ::error,
        warn = ::warn,
        info = ::info,
        debug = ::debug
    )
}


internal fun GradleLogger.processLogMessage(
    message: String,
    type: LogType
) {
    processLogMessageInternal(
        message = message,
        type = type,
        error = ::error,
        warn = ::warn,
        info = ::info,
        debug = ::debug
    )
}

private fun processLogMessageInternal(
    message: String,
    type: LogType,
    error: (text: String) -> Unit,
    warn: (text: String) -> Unit,
    info: (text: String) -> Unit,
    debug: (text: String) -> Unit
) {
    val nonColoredMessage = message.clearAnsiColor()
    when (type) {
        LogType.WARN -> {
            warn(nonColoredMessage)
        }
        LogType.ERROR -> {
            error(nonColoredMessage)
        }
        LogType.INFO, LogType.LOG -> info(nonColoredMessage)
        LogType.DEBUG -> debug(nonColoredMessage)
    }
}

enum class LogType(val value: String) {
    ERROR("error"),
    WARN("warn"),
    INFO("info"),
    DEBUG("debug"),
    LOG("log");

    fun isErrorLike(): Boolean {
        return this == ERROR || this == WARN
    }

    companion object {
        fun byValueOrNull(value: String?): LogType? {
            if (value == null) return null
            return values().singleOrNull { it.value == value }
        }
    }
}
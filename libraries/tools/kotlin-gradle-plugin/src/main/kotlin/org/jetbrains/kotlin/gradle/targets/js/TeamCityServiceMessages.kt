/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.utils.clearAnsiColor
import org.gradle.api.logging.Logger as GradleLogger
import org.slf4j.Logger as SlfLogger

internal fun SlfLogger.processLogMessage(
    message: String,
    type: String
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
    type: String
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
    type: String,
    error: (text: String) -> Unit,
    warn: (text: String) -> Unit,
    info: (text: String) -> Unit,
    debug: (text: String) -> Unit
) {
    val nonColoredMessage = message.clearAnsiColor()
    when (type.toLowerCase()) {
        WARN -> {
            warn(nonColoredMessage)
        }
        ERROR -> {
            error(nonColoredMessage)
        }
        INFO, LOG -> info(nonColoredMessage)
        DEBUG -> debug(nonColoredMessage)
    }
}

internal const val ERROR = "error"
internal const val WARN = "warn"
internal const val INFO = "info"
internal const val DEBUG = "debug"
internal const val LOG = "log"

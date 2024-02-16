/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.util.Logger
import kotlin.system.exitProcess

internal object KlibToolLogger : Logger, IrMessageLogger {
    override fun log(message: String) = println(message)
    override fun warning(message: String) = logWarning(message)
    override fun error(message: String) = logWarning(message)

    @Deprecated(Logger.FATAL_DEPRECATION_MESSAGE, ReplaceWith(Logger.FATAL_REPLACEMENT))
    override fun fatal(message: String) = logError(message, withStacktrace = true)

    override fun report(severity: IrMessageLogger.Severity, message: String, location: IrMessageLogger.Location?) {
        when (severity) {
            IrMessageLogger.Severity.INFO -> log(message)
            IrMessageLogger.Severity.WARNING -> warning(message)
            IrMessageLogger.Severity.ERROR -> error(message)
        }
    }
}

internal fun logWarning(text: String) {
    println("warning: $text")
}

internal fun logError(text: String, withStacktrace: Boolean = false): Nothing {
    if (withStacktrace)
        error("error: $text")
    else {
        System.err.println("error: $text")
        exitProcess(1)
    }
}

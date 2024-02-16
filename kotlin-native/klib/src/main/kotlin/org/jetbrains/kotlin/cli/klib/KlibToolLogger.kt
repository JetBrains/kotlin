/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.util.Logger

internal class KlibToolLogger(private val output: KlibToolOutput) : Logger, IrMessageLogger {
    override fun log(message: String) {
        output.logInfo(message)
    }

    override fun warning(message: String) {
        output.logWarning(message)
    }

    override fun error(message: String) {
        output.logError(message)
    }

    @Deprecated(Logger.FATAL_DEPRECATION_MESSAGE, ReplaceWith(Logger.FATAL_REPLACEMENT))
    override fun fatal(message: String) = throw IllegalStateException("error: $message")

    override fun report(severity: IrMessageLogger.Severity, message: String, location: IrMessageLogger.Location?) {
        when (severity) {
            IrMessageLogger.Severity.INFO -> log(message)
            IrMessageLogger.Severity.WARNING -> warning(message)
            IrMessageLogger.Severity.ERROR -> error(message)
        }
    }
}

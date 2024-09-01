/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.util.Logger

internal class KlibToolLogger(private val output: KlibToolOutput) : Logger, MessageCollector {
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

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        when (severity) {
            CompilerMessageSeverity.INFO, CompilerMessageSeverity.LOGGING, CompilerMessageSeverity.OUTPUT -> log(message)
            CompilerMessageSeverity.WARNING -> warning(message)
            CompilerMessageSeverity.STRONG_WARNING -> strongWarning(message)
            CompilerMessageSeverity.ERROR, CompilerMessageSeverity.EXCEPTION -> error(message)
        }
    }

    override fun clear() {}

    override fun hasErrors() = output.hasErrors
}
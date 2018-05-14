/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.util

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import java.io.PrintWriter
import java.io.StringWriter

class MessageCollectorBackedKaptLogger(
    override val isVerbose: Boolean,
    val messageCollector: MessageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, isVerbose)
) : KaptLogger {
    private companion object {
        val PREFIX = "[kapt] "
    }

    override val errorWriter = makeWriter(CompilerMessageSeverity.ERROR)
    override val warnWriter = makeWriter(CompilerMessageSeverity.STRONG_WARNING)
    override val infoWriter = makeWriter(CompilerMessageSeverity.WARNING)

    override fun info(message: String) {
        if (isVerbose) {
            messageCollector.report(CompilerMessageSeverity.INFO, PREFIX + message)
        }
    }
    
    override fun warn(message: String) {
        messageCollector.report(CompilerMessageSeverity.WARNING, PREFIX + message)
    }

    override fun error(message: String) {
        messageCollector.report(CompilerMessageSeverity.ERROR, PREFIX + message)
    }

    override fun exception(e: Throwable) {
        val stacktrace = run {
            val writer = StringWriter()
            e.printStackTrace(PrintWriter(writer))
            writer.toString()
        }
        messageCollector.report(CompilerMessageSeverity.ERROR, PREFIX + "An exception occurred: " + stacktrace)
    }

    private fun makeWriter(severity: CompilerMessageSeverity): PrintWriter {
        return PrintWriter(MessageCollectorBackedWriter(messageCollector, severity))
    }
}

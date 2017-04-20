/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt3.util

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import java.io.PrintWriter
import java.io.StringWriter

class KaptLogger(
        val isVerbose: Boolean,
        val messageCollector: MessageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, isVerbose)
) {
    private companion object {
        val PREFIX = "[kapt] "
    }

    fun info(message: String) {
        if (isVerbose) {
            messageCollector.report(CompilerMessageSeverity.INFO, PREFIX + message)
        }
    }

    inline fun info(message: () -> String) {
        if (isVerbose) {
            info(message())
        }
    }

    fun warn(message: String) {
        messageCollector.report(CompilerMessageSeverity.WARNING, PREFIX + message)
    }

    fun error(message: String) {
        messageCollector.report(CompilerMessageSeverity.ERROR, PREFIX + message)
    }

    fun exception(e: Throwable) {
        val stacktrace = run {
            val writer = StringWriter()
            e.printStackTrace(PrintWriter(writer))
            writer.toString()
        }
        messageCollector.report(CompilerMessageSeverity.ERROR, PREFIX + "An exception occurred: " + stacktrace)
    }
}

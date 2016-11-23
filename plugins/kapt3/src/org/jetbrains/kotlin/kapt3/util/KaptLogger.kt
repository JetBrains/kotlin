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

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

class KaptLogger(val isVerbose: Boolean, val messageCollector: MessageCollector) {
    private companion object {
        val PREFIX = "[kapt] "
    }

    fun info(message: String) {
        if (isVerbose) {
            messageCollector.report(CompilerMessageSeverity.INFO, PREFIX + message, CompilerMessageLocation.NO_LOCATION)
        }
    }

    inline fun info(message: () -> String) {
        if (isVerbose) {
            info(message())
        }
    }

    fun warn(message: String) {
        println(PREFIX + message)
    }

    fun error(message: String) {
        messageCollector.report(CompilerMessageSeverity.ERROR, PREFIX + message, CompilerMessageLocation.NO_LOCATION)
    }

    fun exception(e: Throwable) {
        val stacktrace = run {
            val byteArrayOutputStream = ByteArrayOutputStream()
            e.printStackTrace(PrintWriter(byteArrayOutputStream))
            byteArrayOutputStream.toString("UTF-8")
        }
        messageCollector.report(CompilerMessageSeverity.EXCEPTION, PREFIX + "An exception occurred: " + stacktrace, CompilerMessageLocation.NO_LOCATION)

    }
}
/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.daemon.client.CompilerCallbackServicesFacadeServer
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.Serializable

internal class JpsCompilerServicesFacadeImpl(
        private val env: JpsCompilerEnvironment,
        port: Int = SOCKET_ANY_FREE_PORT
) : CompilerCallbackServicesFacadeServer(env.services.get(IncrementalCompilationComponents::class.java),
                                         env.services.get(CompilationCanceledStatus::class.java),
                                         port),
        JpsCompilerServicesFacade {

    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        val reportCategory = ReportCategory.fromCode(category)

        when (reportCategory) {
            ReportCategory.OUTPUT_MESSAGE -> {
                env.messageCollector.report(CompilerMessageSeverity.OUTPUT, message!!, CompilerMessageLocation.NO_LOCATION)
            }
            ReportCategory.EXCEPTION -> {
                env.messageCollector.report(CompilerMessageSeverity.EXCEPTION, message!!, CompilerMessageLocation.NO_LOCATION)
            }
            ReportCategory.COMPILER_MESSAGE -> {
                val compilerSeverity = when (ReportSeverity.fromCode(severity)) {
                    ReportSeverity.ERROR -> CompilerMessageSeverity.ERROR
                    ReportSeverity.WARNING -> CompilerMessageSeverity.WARNING
                    ReportSeverity.INFO -> CompilerMessageSeverity.INFO
                    ReportSeverity.DEBUG -> CompilerMessageSeverity.LOGGING
                    else -> throw IllegalStateException("Unexpected compiler message report severity $severity")
                }
                if (message != null && attachment is CompilerMessageLocation) {
                    env.messageCollector.report(compilerSeverity, message, attachment)
                }
                else {
                    reportUnexpected(category, severity, message, attachment)
                }
            }
            ReportCategory.DAEMON_MESSAGE,
            ReportCategory.IC_MESSAGE -> {
                if (message != null) {
                    env.messageCollector.report(CompilerMessageSeverity.LOGGING, message, CompilerMessageLocation.NO_LOCATION)
                }
                else {
                    reportUnexpected(category, severity, message, attachment)
                }
            }
            else -> {
                reportUnexpected(category, severity, message, attachment)
            }
        }
    }

    private fun reportUnexpected(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        val compilerMessageSeverity = when (ReportSeverity.fromCode(severity)) {
            ReportSeverity.ERROR -> CompilerMessageSeverity.ERROR
            ReportSeverity.WARNING -> CompilerMessageSeverity.WARNING
            ReportSeverity.INFO -> CompilerMessageSeverity.INFO
            else -> CompilerMessageSeverity.LOGGING
        }

        env.messageCollector.report(compilerMessageSeverity,
                                    "Unexpected message: category=$category; severity=$severity; message='$message'; attachment=$attachment",
                                    CompilerMessageLocation.NO_LOCATION)
    }
}
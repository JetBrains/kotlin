/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

internal open class DefaultLlvmDiagnosticHandler(
        private val loggingContext: LoggingContext,
        private val messageCollector: MessageCollector,
        private val policy: Policy = Policy.Default,
) : LlvmDiagnosticHandler {
    interface Policy {
        fun suppressWarning(diagnostic: LlvmDiagnostic): Boolean = false

        object Default : Policy
    }

    override fun handle(diagnostics: List<LlvmDiagnostic>) {
        diagnostics.forEach {
            when (it.severity) {
                LlvmDiagnostic.Severity.ERROR -> throw Error(it.message)
                LlvmDiagnostic.Severity.WARNING -> if (loggingContext.inVerbosePhase || !policy.suppressWarning(it)) {
                    messageCollector.report(CompilerMessageSeverity.WARNING, it.message)
                } else {
                    // else block is required by the compiler.
                }
                LlvmDiagnostic.Severity.REMARK,
                LlvmDiagnostic.Severity.NOTE -> {
                    loggingContext.log { "${it.severity}: ${it.message}" }
                }
            }.also {} // Make exhaustive.
        }
    }
}

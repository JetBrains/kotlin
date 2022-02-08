/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity

internal open class DefaultLlvmDiagnosticHandler(
        private val context: Context,
        private val policy: Policy = Policy.Default
) : LlvmDiagnosticHandler {
    interface Policy {
        fun suppressWarning(diagnostic: LlvmDiagnostic): Boolean = false

        object Default : Policy
    }

    override fun handle(diagnostics: List<LlvmDiagnostic>) {
        diagnostics.forEach {
            when (it.severity) {
                LlvmDiagnostic.Severity.ERROR -> throw Error(it.message)
                LlvmDiagnostic.Severity.WARNING -> if (context.inVerbosePhase || !policy.suppressWarning(it)) {
                    context.messageCollector.report(CompilerMessageSeverity.WARNING, it.message)
                } else {
                    // else block is required by the compiler.
                }
                LlvmDiagnostic.Severity.REMARK,
                LlvmDiagnostic.Severity.NOTE -> {
                    context.log { "${it.severity}: ${it.message}" }
                }
            }.also {} // Make exhaustive.
        }
    }
}

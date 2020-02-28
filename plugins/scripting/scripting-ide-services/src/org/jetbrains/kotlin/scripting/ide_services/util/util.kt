/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.util

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import kotlin.script.experimental.api.*

data class CompiledErrors(
    val message: String,
    val location: CompilerMessageLocation?
)

fun <T> ResultWithDiagnostics<T>.getErrors(): CompiledErrors =
    CompiledErrors(
        reports.joinToString("\n") { report ->
            report.location?.let { loc ->
                CompilerMessageLocation.create(
                    report.sourcePath,
                    loc.start.line,
                    loc.start.col,
                    loc.end?.line,
                    loc.end?.col,
                    null
                )?.toString()?.let {
                    "$it "
                }
            }.orEmpty() + report.message
        },
        reports.firstOrNull {
            when (it.severity) {
                ScriptDiagnostic.Severity.ERROR -> true
                ScriptDiagnostic.Severity.FATAL -> true
                else -> false
            }
        }?.let {
            val loc = it.location ?: return@let null
            CompilerMessageLocation.create(
                it.sourcePath,
                loc.start.line,
                loc.start.col,
                loc.end?.line,
                loc.end?.col,
                null
            )
        }
    )
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.util

import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic

fun <T> ResultWithDiagnostics<T>.isIncomplete() = this.reports.any { it.code == ScriptDiagnostic.incompleteCode }

fun <T> ResultWithDiagnostics<T>.hasErrors() = this is ResultWithDiagnostics.Failure

fun <T> ResultWithDiagnostics<T>.renderError(
    withSeverity: Boolean = true,
    withLocation: Boolean = true,
    withException: Boolean = true,
    withStackTrace: Boolean = false
) =
    this.reports.joinToString("\n") {
        it.render(withSeverity, withLocation, withException, withStackTrace)
    }

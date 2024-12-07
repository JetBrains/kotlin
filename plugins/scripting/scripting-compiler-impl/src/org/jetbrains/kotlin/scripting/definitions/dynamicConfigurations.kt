/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.valueOr

private fun ScriptCompilationConfigurationResult.valueOrError() = valueOr { failure ->
    val singleCause = failure.reports.singleOrNull { it.severity == ScriptDiagnostic.Severity.ERROR }
    if (singleCause != null)
        throw IllegalStateException(singleCause.message, singleCause.exception)
    else
        throw IllegalStateException(
            "Error retrieving script compilation configuration: ${failure.reports.joinToString { it.message }}",
            failure.reports.find { it.exception != null }?.exception
        )
}

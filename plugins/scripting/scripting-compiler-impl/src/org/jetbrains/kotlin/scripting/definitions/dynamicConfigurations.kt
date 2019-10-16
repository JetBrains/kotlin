/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.valueOr

fun PsiFile.findScriptCompilationConfiguration(): ScriptCompilationConfiguration? {
    // Do not use psiFile.script, see comments in findScriptDefinition
    if (this !is KtFile/* || this.script == null*/) return null
    val file = virtualFile ?: originalFile.virtualFile ?: return null
    if (file.isNonScript()) return null

    val provider = ScriptDependenciesProvider.getInstance(project)
    // Ignoring the error here, assuming that it will be reported elsewhere anyway (this is important scenario in IDE)
    return provider?.getScriptConfiguration(this)?.configuration
        ?: findScriptDefinition()?.compilationConfiguration
}

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

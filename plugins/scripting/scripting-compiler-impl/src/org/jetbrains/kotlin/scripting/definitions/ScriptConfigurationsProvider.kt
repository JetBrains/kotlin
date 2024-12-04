/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.valueOrNull

// TODO: support SourceCode (or KtSourceFile) as a key
open class ScriptConfigurationsProvider(protected val project: Project) {
    open fun getScriptConfigurationResult(file: KtFile): ScriptCompilationConfigurationResult? = null

    // TODO: consider fixing implementations and removing default implementation
    open fun getScriptConfigurationResult(
        file: KtFile, providedConfiguration: ScriptCompilationConfiguration?,
    ): ScriptCompilationConfigurationResult? = getScriptConfigurationResult(file)

    open fun getScriptConfiguration(file: KtFile): ScriptCompilationConfigurationWrapper? =
        getScriptConfigurationResult(file)?.valueOrNull()

    companion object {
        fun getInstance(project: Project): ScriptConfigurationsProvider? =
            project.getService(ScriptConfigurationsProvider::class.java)
    }
}

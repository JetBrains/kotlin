/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.valueOrNull

@RequiresOptIn(message = "For K2 use session-based scriptDefinitionProviderService", level = RequiresOptIn.Level.ERROR)
annotation class K1SpecificScriptingServiceAccessor


// TODO: support SourceCode (or KtSourceFile) as a key
open class ScriptConfigurationsProvider(protected val project: Project) {

    /**
     * Currently the main method that should be used to get the configuration, others should be deprecated
     */
    open fun getScriptCompilationConfiguration(
        scriptSource: SourceCode,
        providedConfiguration: ScriptCompilationConfiguration? = null,
    ): ScriptCompilationConfigurationResult? =
        (scriptSource as? KtFileScriptSource)?.ktFile?.let {
            // transitioning to the new API based on the generic source file representation
            @Suppress("DEPRECATION")
            getScriptConfigurationResult(it)
        }

    @Deprecated("Use getScriptCompilationConfiguration(KtFileScriptSource(ktFile)) instead")
    open fun getScriptConfigurationResult(file: KtFile): ScriptCompilationConfigurationResult? = null

    // TODO: consider fixing implementations and removing default implementation
    @Deprecated("Use getScriptConfigurationResult(KtFileScriptSource(ktFile), provided configuration) instead")
    open fun getScriptConfigurationResult(
        file: KtFile, providedConfiguration: ScriptCompilationConfiguration?,
    ): ScriptCompilationConfigurationResult? {
        @Suppress("DEPRECATION")
        return getScriptConfigurationResult(file)
    }

    @Deprecated("Use getScriptConfigurationResult(KtFileScriptSource(ktFile)) instead")
    open fun getScriptConfiguration(file: KtFile): ScriptCompilationConfigurationWrapper? {
        @Suppress("DEPRECATION")
        return getScriptConfigurationResult(file)?.valueOrNull()
    }

    companion object {
        @K1SpecificScriptingServiceAccessor
        fun getInstance(project: Project): ScriptConfigurationsProvider? =
            project.getService(ScriptConfigurationsProvider::class.java)
    }
}

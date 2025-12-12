/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.valueOrNull

@RequiresOptIn(
    message = "For K2 use scripting host configuration based helpers (e.g. getRefinedCompilationConfiguration)",
    // TODO: switch to ERROR after IntelliJ code migration
    level = RequiresOptIn.Level.WARNING
)
annotation class K1SpecificScriptingServiceAccessor

// TODO: deprecate/optin in favor of K2 infrastructure (ScriptRefinedCompilationConfigurationCache for this one)
open class ScriptConfigurationsProvider(
    @property:K1SpecificScriptingServiceAccessor
    protected val project: Project
) {

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
    @Deprecated("Use getScriptCompilationConfiguration(KtFileScriptSource(ktFile), provided configuration) instead")
    open fun getScriptConfigurationResult(
        file: KtFile, providedConfiguration: ScriptCompilationConfiguration?,
    ): ScriptCompilationConfigurationResult? {
        return getScriptConfigurationResult(file)
    }

    @Deprecated("Use getScriptCompilationConfiguration(KtFileScriptSource(ktFile)) instead")
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

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.K1SpecificScriptingServiceAccessor
import org.jetbrains.kotlin.scripting.definitions.ScriptConfigurationsProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.resolve.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode

class CliScriptConfigurationsProvider(
    getScriptDefinitionProvider: () -> ScriptDefinitionProvider
) : ScriptConfigurationsProvider() {
    private val cacheLock = ReentrantReadWriteLock()

    private val cache = hashMapOf<String, ScriptCompilationConfigurationResult?>()

    private val knownVirtualFileSources = mutableMapOf<String, VirtualFileScriptSource>()
    private val scriptDefinitionProvider by lazy(LazyThreadSafetyMode.NONE) {
        getScriptDefinitionProvider()
    }

    @Deprecated("Use getScriptConfigurationResult(KtFileScriptSource(ktFile)) instead")
    override fun getScriptConfigurationResult(project: Project, file: KtFile): ScriptCompilationConfigurationResult? = cacheLock.read {
        calculateRefinedConfiguration(project, KtFileScriptSource(file), null)
    }

    @Deprecated("Use getScriptConfigurationResult(KtFileScriptSource(ktFile), providedConfiguration) instead")
    override fun getScriptConfigurationResult(
        project: Project,
        file: KtFile,
        providedConfiguration: ScriptCompilationConfiguration?
    ): ScriptCompilationConfigurationResult? = cacheLock.read {
        calculateRefinedConfiguration(project, KtFileScriptSource(file), providedConfiguration)
    }

    override fun getScriptCompilationConfiguration(
        project: Project,
        scriptSource: SourceCode,
        providedConfiguration: ScriptCompilationConfiguration?,
    ): ScriptCompilationConfigurationResult? = cacheLock.read {
        calculateRefinedConfiguration(project, scriptSource, providedConfiguration)
    }

    @OptIn(K1SpecificScriptingServiceAccessor::class)
    private fun calculateRefinedConfiguration(
        project: Project, source: SourceCode, providedConfiguration: ScriptCompilationConfiguration?
    ): ScriptCompilationConfigurationResult? {
        val path = source.locationId ?: return null
        val cached = cache[path]
        return if (cached != null) cached
        else {
            val scriptDef = scriptDefinitionProvider.findDefinition(source)
            if (scriptDef != null) {
                val result =
                    refineScriptCompilationConfiguration(
                        source, scriptDef, project, providedConfiguration, knownVirtualFileSources
                    )

                if (source is VirtualFileScriptSource) {
                    project.getService(ScriptReportSink::class.java)?.attachReports(source.virtualFile, result.reports)
                }

                cacheLock.write {
                    cache.put(path, result)
                }
                result
            } else null
        }
    }
}

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.PsiScriptAnnotationsCollector
import org.jetbrains.kotlin.scripting.definitions.K1SpecificScriptingServiceAccessor
import org.jetbrains.kotlin.scripting.definitions.ScriptConfigurationsProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.runReadAction
import org.jetbrains.kotlin.scripting.resolve.*
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.onSuccess

class CliScriptConfigurationsProvider(
    project: Project?,
    // the compilation classpath, requested only if a script has annotations to resolve
    getRegularClasspath: () -> List<File>,
    getScriptDefinitionProvider: () -> ScriptDefinitionProvider
) : ScriptConfigurationsProvider(project) {

    @Suppress("unused") // Could be used in the wild
    constructor(project: Project?, getScriptDefinitionProvider: () -> ScriptDefinitionProvider) :
            this(project, { emptyList() }, getScriptDefinitionProvider)

    private val cacheLock = ReentrantReadWriteLock()

    private val cache = hashMapOf<String, ScriptCompilationConfigurationResult?>()

    private val knownVirtualFileSources = mutableMapOf<String, VirtualFileScriptSource>()
    private val scriptDefinitionProvider by lazy(LazyThreadSafetyMode.NONE) {
        getScriptDefinitionProvider()
    }

    var reportSink: ScriptReportSink? = null

    private val annotationsCollector = PsiScriptAnnotationsCollector { getRegularClasspath() }

    @Deprecated("Use getScriptConfigurationResult(KtFileScriptSource(ktFile)) instead")
    override fun getScriptConfigurationResult(file: KtFile): ScriptCompilationConfigurationResult? = cacheLock.read {
        calculateRefinedConfiguration(KtFileScriptSource(file), null)
    }

    @Deprecated("Use getScriptConfigurationResult(KtFileScriptSource(ktFile), providedConfiguration) instead")
    override fun getScriptConfigurationResult(
        file: KtFile,
        providedConfiguration: ScriptCompilationConfiguration?
    ): ScriptCompilationConfigurationResult? = cacheLock.read {
        calculateRefinedConfiguration(KtFileScriptSource(file), providedConfiguration)
    }

    override fun getScriptCompilationConfiguration(
        scriptSource: SourceCode,
        providedConfiguration: ScriptCompilationConfiguration?,
    ): ScriptCompilationConfigurationResult? = cacheLock.read {
        calculateRefinedConfiguration(scriptSource, providedConfiguration)
    }

    @OptIn(K1SpecificScriptingServiceAccessor::class)
    private fun calculateRefinedConfiguration(
        source: SourceCode,
        providedConfiguration: ScriptCompilationConfiguration?
    ): ScriptCompilationConfigurationResult? {
        val path = source.locationId ?: return null
        val cached = cache[path]
        return if (cached != null) cached
        else {
            val scriptDef = scriptDefinitionProvider.findDefinition(source)
            if (scriptDef != null) {
                val compilationConfiguration = providedConfiguration ?: scriptDef.compilationConfiguration
                val ktFile = source.toKtFileSource(scriptDef, project).ktFile
                val hostConfiguration = compilationConfiguration.hostConfigurationOrDefault(scriptDef.contextClassLoader)
                val result = runReadAction {
                    annotationsCollector.collectAnnotations(ktFile, compilationConfiguration, hostConfiguration)
                }.onSuccess { collectedData ->
                    refineScriptCompilationConfiguration(
                        compilationConfiguration, source, collectedData, knownVirtualFileSources, scriptDef
                    )
                }

                if (source is VirtualFileScriptSource) {
                    (reportSink ?: project.getService(ScriptReportSink::class.java))?.attachReports(source.virtualFile, result.reports)
                }

                cacheLock.write {
                    cache.put(path, result)
                }
                result
            } else null
        }
    }
}

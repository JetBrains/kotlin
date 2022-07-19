/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.experimental.api.ScriptCompilationConfiguration

class CliScriptDependenciesProvider(project: Project) : ScriptDependenciesProvider(project) {
    private val cacheLock = ReentrantReadWriteLock()
    private val cache = hashMapOf<String, ScriptCompilationConfigurationResult?>()
    private val knownVirtualFileSources = mutableMapOf<String, VirtualFileScriptSource>()

    override fun getScriptConfigurationResult(file: KtFile): ScriptCompilationConfigurationResult? = cacheLock.read {
        calculateRefinedConfiguration(file, null)
    }

    override fun getScriptConfigurationResult(
        file: KtFile,
        providedConfiguration: ScriptCompilationConfiguration?
    ): ScriptCompilationConfigurationResult? = cacheLock.read {
        calculateRefinedConfiguration(file, providedConfiguration)
    }

    private fun calculateRefinedConfiguration(
        file: KtFile, providedConfiguration: ScriptCompilationConfiguration?
    ): ScriptCompilationConfigurationResult? {
        val path = file.virtualFilePath
        val cached = cache[path]
        return if (cached != null) cached
        else {
            val scriptDef = file.findScriptDefinition()
            if (scriptDef != null) {
                val result =
                    refineScriptCompilationConfiguration(
                        KtFileScriptSource(file), scriptDef, project, providedConfiguration, knownVirtualFileSources
                    )

                project.getService(ScriptReportSink::class.java)?.attachReports(file.virtualFile, result.reports)

                cacheLock.write {
                    cache.put(path, result)
                }
                result
            } else null
        }
    }
}

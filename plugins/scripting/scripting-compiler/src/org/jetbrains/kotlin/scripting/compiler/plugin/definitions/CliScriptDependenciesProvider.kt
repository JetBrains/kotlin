/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptReportSink
import org.jetbrains.kotlin.scripting.resolve.refineScriptCompilationConfiguration
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.experimental.api.ResultWithDiagnostics

class CliScriptDependenciesProvider(project: Project) : ScriptDependenciesProvider(project) {
    private val cacheLock = ReentrantReadWriteLock()
    private val cache = hashMapOf<String, ScriptCompilationConfigurationResult?>()

    override fun getScriptConfigurationResult(file: KtFile): ScriptCompilationConfigurationResult? = cacheLock.read {
        calculateRefinedConfiguration(file)
    }

    private fun calculateRefinedConfiguration(file: KtFile): ScriptCompilationConfigurationResult? {
        val path = file.virtualFilePath
        val cached = cache[path]
        return if (cached != null) cached
        else {
            val scriptDef = file.findScriptDefinition()
            if (scriptDef != null) {
                val result = refineScriptCompilationConfiguration(KtFileScriptSource(file), scriptDef, project)

                ServiceManager.getService(project, ScriptReportSink::class.java)?.attachReports(file.virtualFile, result.reports)

                if (result is ResultWithDiagnostics.Success) {
                    log.info("[kts] new cached deps for $path: ${result.value.dependenciesClassPath.joinToString(File.pathSeparator)}")
                } else {
                    log.info("[kts] new cached errors for $path:\n  ${result.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}")
                }
                cacheLock.write {
                    cache.put(path, result)
                }
                result
            } else null
        }
    }
}

private val log = Logger.getInstance(ScriptDependenciesProvider::class.java)

/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptContentLoader
import org.jetbrains.kotlin.scripting.resolve.ScriptReportSink
import org.jetbrains.kotlin.scripting.resolve.adjustByDefinition
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.experimental.dependencies.ScriptDependencies

class CliScriptDependenciesProvider(private val project: Project) : ScriptDependenciesProvider {
    private val cacheLock = ReentrantReadWriteLock()
    private val cache = hashMapOf<String, ScriptDependencies?>()
    private val scriptContentLoader = ScriptContentLoader(project)

    override fun getScriptDependencies(file: VirtualFile): ScriptDependencies? = cacheLock.read {
        calculateExternalDependencies(file)
    }

    private fun calculateExternalDependencies(file: VirtualFile): ScriptDependencies? {
        val path = file.path
        val cached = cache[path]
        return if (cached != null) cached
        else {
            val scriptDef = file.findScriptDefinition(project)
            if (scriptDef != null) {
                val result = scriptContentLoader.loadContentsAndResolveDependencies(scriptDef, file)

                ServiceManager.getService(project, ScriptReportSink::class.java)?.attachReports(file, result.reports)

                val deps = result.dependencies?.adjustByDefinition(scriptDef)

                if (deps != null) {
                    log.info("[kts] new cached deps for $path: ${deps.classpath.joinToString(File.pathSeparator)}")
                }
                cacheLock.write {
                    cache.put(path, deps)
                }
                deps
            } else null
        }
    }
}

private val log = Logger.getInstance(ScriptDependenciesProvider::class.java)

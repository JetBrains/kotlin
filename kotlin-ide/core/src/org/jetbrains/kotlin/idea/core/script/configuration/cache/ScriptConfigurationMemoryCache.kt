/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.cache

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.SLRUMap
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

open class ScriptConfigurationMemoryCache(
    val project: Project
) : ScriptConfigurationCache {
    companion object {
        const val MAX_SCRIPTS_CACHED = 50
    }

    private val memoryCache = SLRUMap<VirtualFile, ScriptConfigurationState>(MAX_SCRIPTS_CACHED, MAX_SCRIPTS_CACHED)

    @Synchronized
    override operator fun get(file: VirtualFile): ScriptConfigurationState? {
        return memoryCache.get(file)
    }

    @Synchronized
    override fun setApplied(file: VirtualFile, configurationSnapshot: ScriptConfigurationSnapshot) {
        val old = memoryCache[file] ?: ScriptConfigurationState()
        memoryCache.put(file, old.copy(applied = configurationSnapshot))
    }

    @Synchronized
    override fun setLoaded(file: VirtualFile, configurationSnapshot: ScriptConfigurationSnapshot) {
        val old = memoryCache[file] ?: ScriptConfigurationState()
        memoryCache.put(file, old.copy(loaded = configurationSnapshot))
    }

    @Synchronized
    override fun markOutOfDate(scope: ScriptConfigurationCacheScope) {
        when (scope) {
            is ScriptConfigurationCacheScope.File -> {
                val file = scope.file.originalFile.virtualFile
                markFileOutOfDate(file)
            }
            is ScriptConfigurationCacheScope.Except -> {
                val file = scope.file.originalFile.virtualFile
                memoryCache.entrySet().forEach {
                    if (it.key != file) {
                        markFileOutOfDate(it.key)
                    }
                }
            }
            is ScriptConfigurationCacheScope.All ->{
                memoryCache.entrySet().forEach {
                    markFileOutOfDate(it.key)
                }
            }
        }
    }

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    override fun allApplied(): Map<VirtualFile, ScriptCompilationConfigurationWrapper> {
        val result = hashMapOf<VirtualFile, ScriptCompilationConfigurationWrapper>()
        for ((file, configuration) in memoryCache.entrySet()) {
            if (configuration.applied?.configuration != null) {
                result[file] = configuration.applied.configuration
            }
        }
        return result
    }

    @Synchronized
    override fun clear() {
        memoryCache.clear()
    }

    @Synchronized
    private fun markFileOutOfDate(file: VirtualFile) {
        val old = memoryCache[file] ?: return
        memoryCache.put(
            file, old.copy(
                applied = old.applied?.copy(inputs = CachedConfigurationInputs.OutOfDate),
                loaded = old.loaded?.copy(inputs = CachedConfigurationInputs.OutOfDate)
            )
        )
    }
}
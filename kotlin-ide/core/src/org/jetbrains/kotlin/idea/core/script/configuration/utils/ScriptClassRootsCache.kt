/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.containers.ConcurrentFactoryMap
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.io.File

internal class ScriptClassRootsCache(
    private val project: Project,
    private val all: Map<VirtualFile, ScriptCompilationConfigurationWrapper>
) {
    private val scriptsSdksCache: Map<VirtualFile, Sdk?> =
        ConcurrentFactoryMap.createWeakMap { file ->
            return@createWeakMap getScriptSdk(all[file]) ?: ScriptConfigurationManager.getScriptDefaultSdk(project)
        }

    fun getScriptSdk(file: VirtualFile): Sdk? = scriptsSdksCache[file]

    private fun getScriptSdk(compilationConfiguration: ScriptCompilationConfigurationWrapper?): Sdk? {
        // workaround for mismatched gradle wrapper and plugin version
        val javaHome = try {
            compilationConfiguration?.javaHome?.let { VfsUtil.findFileByIoFile(it, true) }
        } catch (e: Throwable) {
            null
        } ?: return null

        return getAllProjectSdks().find { it.homeDirectory == javaHome }
    }

    val firstScriptSdk: Sdk? by lazy {
        val firstCachedScript = all.keys.firstOrNull() ?: return@lazy null
        return@lazy getScriptSdk(firstCachedScript)
    }

    private fun Sdk.isAlreadyIndexed(): Boolean {
        return ModuleManager.getInstance(project).modules.any { ModuleRootManager.getInstance(it).sdk == this }
    }

    val allDependenciesClassFiles by lazy {
        ScriptClassRootsStorage.getInstance(project).loadClasspathRoots()
    }

    val allDependenciesSources by lazy {
        ScriptClassRootsStorage.getInstance(project).loadSourcesRoots()
    }

    val allDependenciesClassFilesScope by lazy {
        NonClasspathDirectoriesScope.compose(allDependenciesClassFiles)
    }

    val allDependenciesSourcesScope by lazy {
        NonClasspathDirectoriesScope.compose(allDependenciesSources)
    }

    private val scriptsDependenciesClasspathScopeCache: MutableMap<VirtualFile, GlobalSearchScope> =
        ConcurrentFactoryMap.createWeakMap { file ->
            val compilationConfiguration = all[file]
                ?: return@createWeakMap GlobalSearchScope.EMPTY_SCOPE

            val roots = compilationConfiguration.dependenciesClassPath
            val sdk = scriptsSdksCache[file]

            @Suppress("FoldInitializerAndIfToElvis")
            if (sdk == null) {
                return@createWeakMap NonClasspathDirectoriesScope.compose(
                    ScriptConfigurationManager.toVfsRoots(
                        roots
                    )
                )
            }

            return@createWeakMap NonClasspathDirectoriesScope.compose(
                sdk.rootProvider.getFiles(OrderRootType.CLASSES).toList() + ScriptConfigurationManager.toVfsRoots(
                    roots
                )
            )
        }

    fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope {
        return scriptsDependenciesClasspathScopeCache[file] ?: GlobalSearchScope.EMPTY_SCOPE
    }

    fun hasNotCachedRoots(compilationConfiguration: ScriptCompilationConfigurationWrapper): Boolean {
        return !ScriptClassRootsStorage.getInstance(project).containsAll(extractRoots(compilationConfiguration))
    }

    private fun extractRoots(configuration: ScriptCompilationConfigurationWrapper): ScriptClassRootsStorage.Companion.ScriptClassRoots {
        val scriptSdk = getScriptSdk(configuration) ?: ScriptConfigurationManager.getScriptDefaultSdk(project)
        if (scriptSdk != null && !scriptSdk.isAlreadyIndexed()) {
            return ScriptClassRootsStorage.Companion.ScriptClassRoots(
                configuration.dependenciesClassPath,
                configuration.dependenciesSources,
                listOf(scriptSdk)
            )
        }

        return ScriptClassRootsStorage.Companion.ScriptClassRoots(
            configuration.dependenciesClassPath,
            configuration.dependenciesSources,
            emptyList()
        )
    }

    init {
        saveClassRootsToStorage()
    }

    private fun saveClassRootsToStorage() {
        if (all.isEmpty()) return

        val classpath = mutableSetOf<File>()
        val sources = mutableSetOf<File>()
        val sdks = mutableSetOf<Sdk>()

        for ((file, configuration) in all) {
            val scriptSdk = getScriptSdk(file)
            if (scriptSdk != null && !scriptSdk.isAlreadyIndexed()) {
                sdks.add(scriptSdk)
            }

            classpath.addAll(configuration.dependenciesClassPath)
            sources.addAll(configuration.dependenciesSources)
        }

        val rootsStorage = ScriptClassRootsStorage.getInstance(project)
        rootsStorage.save(
            ScriptClassRootsStorage.Companion.ScriptClassRoots(
                classpath.toList(),
                sources.toList(),
                sdks.toList()
            )
        )
    }

    fun contains(file: VirtualFile) = all.containsKey(file)
}
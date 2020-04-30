/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.ProjectTopics
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesModificationTracker
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangesNotifier
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsUpdater
import org.jetbrains.kotlin.idea.core.script.configuration.utils.getKtFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

class CompositeScriptConfigurationManager(val project: Project) : ScriptConfigurationManager {
    @Suppress("unused")
    private val notifier = ScriptChangesNotifier(project)

    val updater = ScriptClassRootsUpdater(project, this)

    private val plugins = ScriptingSupport.Provider.EPN.getPoint(project).extensionList

    val default = DefaultScriptingSupport(this)

    fun tryGetScriptDefinitionFast(locationId: String): ScriptDefinition? {
        return classpathRoots.getLightScriptInfo(locationId)?.definition
    }

    private fun getOrLoadConfiguration(
        virtualFile: VirtualFile,
        preloadedKtFile: KtFile? = null
    ): ScriptCompilationConfigurationWrapper? {
        val scriptConfiguration = classpathRoots.getScriptConfiguration(virtualFile)
        if (scriptConfiguration != null) return scriptConfiguration

        // check that this script should be loaded later in special way (e.g. gradle project import)
        if (plugins.any { it.isApplicable(virtualFile) }) return null

        return default.getOrLoadConfiguration(virtualFile, preloadedKtFile)
    }

    override fun getConfiguration(file: KtFile) =
        getOrLoadConfiguration(file.originalFile.virtualFile, file)

    override fun hasConfiguration(file: KtFile): Boolean =
        classpathRoots.contains(file.originalFile.virtualFile)

    override fun isConfigurationLoadingInProgress(file: KtFile): Boolean =
        plugins.firstOrNull { it.isApplicable(file.originalFile.virtualFile) }?.isConfigurationLoadingInProgress(file)
            ?: default.isConfigurationLoadingInProgress(file)

    @Volatile
    private var classpathRoots: ScriptClassRootsCache = recreateRootsCache()

    fun getLightScriptInfo(file: String): ScriptClassRootsCache.LightScriptInfo? =
        classpathRoots.getLightScriptInfo(file)

    private fun recreateRootsCache(): ScriptClassRootsCache {
        val builder = ScriptClassRootsCache.Builder(project)
        default.collectConfigurations(builder)
        plugins.forEach { it.collectConfigurations(builder) }
        return builder.build()
    }

    fun collectRootsAndCheckNew(): ScriptClassRootsCache.Updates {
        val old = classpathRoots
        val new = recreateRootsCache()
        classpathRoots = new
        return new.diff(old)
    }

    override fun updateScriptDefinitions() {
        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()

        default.updateScriptDefinitions()

        if (classpathRoots.customDefinitionsUsed) {
            plugins.forEach {
                it.updateScriptDefinitions()
            }

            updater.ensureUpdateScheduled()
        }
    }

    init {
        val connection = project.messageBus.connect(project)
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                if (event.isCausedByFileTypesChange) return

                if (classpathRoots.hasInvalidSdk(project)) {
                    updater.ensureUpdateScheduled()
                }
            }
        })
    }

    /**
     * Returns script classpath roots
     * Loads script configuration if classpath roots don't contain [file] yet
     */
    private fun getActualClasspathRoots(file: VirtualFile): ScriptClassRootsCache {
        val classpathRoots = classpathRoots
        if (classpathRoots.contains(file)) {
            return classpathRoots
        }

        getOrLoadConfiguration(file)

        return this.classpathRoots
    }

    override fun getScriptSdk(file: VirtualFile): Sdk? =
        getActualClasspathRoots(file).getScriptSdk(file)

    override fun getFirstScriptsSdk(): Sdk? =
        classpathRoots.firstScriptSdk

    override fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope =
        classpathRoots.getScriptDependenciesClassFilesScope(file)

    override fun getAllScriptsDependenciesClassFilesScope(): GlobalSearchScope =
        classpathRoots.allDependenciesClassFilesScope

    override fun getAllScriptDependenciesSourcesScope(): GlobalSearchScope =
        classpathRoots.allDependenciesSourcesScope

    override fun getAllScriptsDependenciesClassFiles(): List<VirtualFile> =
        classpathRoots.allDependenciesClassFiles

    override fun getAllScriptDependenciesSources(): List<VirtualFile> =
        classpathRoots.allDependenciesSources

    ///////////////////
    // Adapters for deprecated API
    //

    @Deprecated("Use getScriptClasspath(KtFile) instead")
    override fun getScriptClasspath(file: VirtualFile): List<VirtualFile> {
        val ktFile = project.getKtFile(file) ?: return emptyList()
        return getScriptClasspath(ktFile)
    }

    override fun getScriptClasspath(file: KtFile): List<VirtualFile> =
        ScriptConfigurationManager.toVfsRoots(getConfiguration(file)?.dependenciesClassPath.orEmpty())
}

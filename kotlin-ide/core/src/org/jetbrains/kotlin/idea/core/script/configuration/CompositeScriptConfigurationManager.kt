/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.ProjectTopics
import com.intellij.codeInsight.daemon.OutsidersPsiFileSupport
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.ProjectJdkTable.JDK_TABLE_TOPIC
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesModificationTracker
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangesNotifier
import org.jetbrains.kotlin.idea.core.script.configuration.utils.getKtFile
import org.jetbrains.kotlin.idea.core.script.ucache.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.core.script.ucache.ScriptClassRootsUpdater
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

/**
 * The [CompositeScriptConfigurationManager] will provide redirection of [ScriptConfigurationManager] calls to the
 * custom [ScriptingSupport] or [DefaultScriptingSupport] if that script lack of custom [ScriptingSupport].
 *
 * The [ScriptConfigurationManager] is implemented by caching all scripts using the [ScriptClassRootsCache].
 * The [ScriptClassRootsCache] is always available and never blacked by the writer, as all writes occurs
 * using the copy-on-write strategy.
 *
 * This cache are loaded on start and will be updating asynchronously using the [updater].
 * Sync updates still my be occurred from the [DefaultScriptingSupport].
 *
 * We are also watching all script documents:
 * [notifier] will call first applicable [ScriptChangesNotifier.listeners] when editor is activated or document changed.
 * Listener should do something to invalidate configuration and schedule reloading.
 */
class CompositeScriptConfigurationManager(val project: Project) : ScriptConfigurationManager {
    @Suppress("unused")
    private val notifier = ScriptChangesNotifier(project)

    private val classpathRoots: ScriptClassRootsCache
        get() = updater.classpathRoots

    private val plugins = ScriptingSupport.EPN.getPoint(project).extensionList

    val default = DefaultScriptingSupport(this)

    val updater = ScriptClassRootsUpdater(project, this) { builder ->
        default.collectConfigurations(builder)
        plugins.forEach { it.collectConfigurations(builder) }
    }

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
        // (and not for syntactic diff files)
        if (!OutsidersPsiFileSupport.isOutsiderFile(virtualFile)) {
            if (plugins.any { it.isApplicable(virtualFile) }) return null
        }

        return default.getOrLoadConfiguration(virtualFile, preloadedKtFile)
    }

    override fun getConfiguration(file: KtFile) =
        getOrLoadConfiguration(file.originalFile.virtualFile, file)

    override fun hasConfiguration(file: KtFile): Boolean =
        classpathRoots.contains(file.originalFile.virtualFile)

    override fun isConfigurationLoadingInProgress(file: KtFile): Boolean =
        plugins.firstOrNull { it.isApplicable(file.originalFile.virtualFile) }?.isConfigurationLoadingInProgress(file)
            ?: default.isConfigurationLoadingInProgress(file)

    fun getLightScriptInfo(file: String): ScriptClassRootsCache.LightScriptInfo? =
        classpathRoots.getLightScriptInfo(file)

    override fun updateScriptDefinitionReferences() {
        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()

        default.updateScriptDefinitionsReferences()

        if (classpathRoots.customDefinitionsUsed) {
            updater.invalidateAndCommit()
        }
    }

    init {
        val connection = project.messageBus.connect(project)

        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                if (event.isCausedByFileTypesChange) return
                updater.checkInvalidSdks()
            }
        })

        connection.subscribe(JDK_TABLE_TOPIC, object : ProjectJdkTable.Listener {
            override fun jdkAdded(jdk: Sdk) = updater.checkInvalidSdks()
            override fun jdkNameChanged(jdk: Sdk, previousName: String) = updater.checkInvalidSdks()
            override fun jdkRemoved(jdk: Sdk) = updater.checkInvalidSdks(remove = jdk)
        })
    }

    /**
     * Returns script classpath roots
     * Loads script configuration if classpath roots don't contain [file] yet
     */
    private fun getActualClasspathRoots(file: VirtualFile): ScriptClassRootsCache {
        try {
            // we should run default loader if this [file] is not cached in [classpathRoots]
            // and it is not supported by any of [plugins]
            // getOrLoadConfiguration will do this
            // (despite that it's result becomes unused, it still may populate [classpathRoots])
            getOrLoadConfiguration(file, null)
        } catch (cancelled: ProcessCanceledException) {
            // read actions may be cancelled if we are called by impatient reader
        }

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

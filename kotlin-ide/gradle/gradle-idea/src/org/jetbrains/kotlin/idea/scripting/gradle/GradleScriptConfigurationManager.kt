/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupport
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupportHelper
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsIndexer
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

data class ConfigurationData(
    val templateClasspath: List<String>,
    val models: List<KotlinDslScriptModel>
)

class Configuration(val data: ConfigurationData) {
    private val scripts: Map<String, KotlinDslScriptModel>

    val sourcePath: MutableSet<String>
    val classFilePath: MutableSet<String> = mutableSetOf()

    init {
        val allModels = data.models

        scripts = allModels.associateBy { it.file }
        sourcePath = allModels.flatMapTo(mutableSetOf()) { it.sourcePath }

        classFilePath.addAll(data.templateClasspath)
        allModels.flatMapTo(classFilePath) { it.classPath }
    }

    fun scriptModel(file: VirtualFile): KotlinDslScriptModel? {
        return scripts[FileUtil.toSystemDependentName(file.path)]
    }
}

class GradleScriptingSupport(
    private val rootsIndexer: ScriptClassRootsIndexer,
    val project: Project,
    val buildRoot: VirtualFile,
    val context: GradleKtsContext,
    val configuration: Configuration
) : ScriptingSupport() {

    init {
        rootsIndexer.transaction {
            if (classpathRoots.hasNotCachedRoots(GradleClassRootsCache.extractRoots(context, configuration, project))) {
                rootsIndexer.markNewRoot()
            }

            clearClassRootsCaches(project)

            ScriptingSupportHelper.updateHighlighting(project) {
                configuration.scriptModel(it) != null
            }
        }

        hideNotificationForProjectImport(project)
    }

    override fun recreateRootsCache() = GradleClassRootsCache(project, context, configuration)

    private fun updateNotification(file: KtFile) {
        val vFile = file.originalFile.virtualFile
        val scriptModel = configuration?.scriptModel(vFile) ?: return

        if (scriptModel.inputs.isUpToDate(project, vFile)) {
            hideNotificationForProjectImport(project)
        } else {
            showNotificationForProjectImport(project)
        }
    }

    override fun clearCaches() {
        // todo: should clear up to date
    }

    override fun hasCachedConfiguration(file: KtFile): Boolean =
        configuration.scriptModel(file.originalFile.virtualFile) != null

    // TODO: can be true during import
    override fun isConfigurationLoadingInProgress(file: KtFile): Boolean = false

    override fun getOrLoadConfiguration(virtualFile: VirtualFile, preloadedKtFile: KtFile?): ScriptCompilationConfigurationWrapper? {
        return classpathRoots.getScriptConfiguration(virtualFile)
    }

    override val updater: ScriptConfigurationUpdater
        get() = object : ScriptConfigurationUpdater {
            override fun ensureUpToDatedConfigurationSuggested(file: KtFile) {
                // do nothing for gradle scripts
            }

            // unused symbol inspection should not initiate loading
            override fun ensureConfigurationUpToDate(files: List<KtFile>): Boolean = true

            override fun suggestToUpdateConfigurationIfOutOfDate(file: KtFile) {
                updateNotification(file)
            }
        }
}

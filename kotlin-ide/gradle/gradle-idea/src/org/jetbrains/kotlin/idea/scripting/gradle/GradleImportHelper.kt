/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.diff.util.DiffUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFileBase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModelResolver
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager
import org.jetbrains.plugins.gradle.service.project.GradlePartialResolverPolicy
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

fun runPartialGradleImport(project: Project) {
    getGradleProjectSettings(project).forEach { gradleProjectSettings ->
        ExternalSystemUtil.refreshProject(
            gradleProjectSettings.externalProjectPath,
            ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
                .projectResolverPolicy(
                    GradlePartialResolverPolicy { it is KotlinDslScriptModelResolver }
                )
        )
    }
}

fun getMissingConfigurationActionText() = KotlinIdeaGradleBundle.message("action.text.load.script.configurations")

fun autoReloadScriptConfigurations(project: Project, file: VirtualFile): Boolean {
    val buildRoot = GradleBuildRootsManager.getInstance(project).getScriptInfo(file)?.buildRoot ?: return false
    return GradleScriptDefinitionsContributor.getDefinitions(project, buildRoot.pathPrefix, buildRoot.data.gradleHome)?.any {
        KotlinScriptingSettings.getInstance(project).autoReloadConfigurations(it)
    } ?: return false
}

fun scriptConfigurationsNeedToBeUpdated(project: Project, file: VirtualFile) {
    if (autoReloadScriptConfigurations(project, file)) {
        runPartialGradleImport(project)
    } else {
        // notification is shown in LoadConfigurationAction
    }
}

fun scriptConfigurationsAreUpToDate(project: Project): Boolean = true

class LoadConfigurationAction : AnAction(
    KotlinIdeaGradleBundle.message("action.text.load.script.configurations"),
    KotlinIdeaGradleBundle.message("action.description.load.script.configurations"),
    KotlinIcons.LOAD_SCRIPT_CONFIGURATION
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        runPartialGradleImport(project)
    }

    override fun update(e: AnActionEvent) {
        ensureValidActionVisibility(e)
    }

    private fun ensureValidActionVisibility(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        e.presentation.isVisible = getNotificationVisibility(editor)
    }

    private fun getNotificationVisibility(editor: Editor): Boolean {
        if (DiffUtil.isDiffEditor(editor)) {
            return false
        }

        val project = editor.project ?: return false
        val file = getKotlinScriptFile(editor) ?: return false

        if (autoReloadScriptConfigurations(project, file)) {
            return false
        }

        return GradleBuildRootsManager.getInstance(project).isConfigurationOutOfDate(file)
    }

    private fun getKotlinScriptFile(editor: Editor): VirtualFile? {
        return FileDocumentManager.getInstance()
            .getFile(editor.document)
            ?.takeIf {
                it !is LightVirtualFileBase
                        && it.isValid
                        && it.fileType == KotlinFileType.INSTANCE
                        && isGradleKotlinScript(it)
            }
    }
}

fun getGradleVersion(project: Project, settings: GradleProjectSettings): String {
    return settings.resolveGradleVersion().version
}
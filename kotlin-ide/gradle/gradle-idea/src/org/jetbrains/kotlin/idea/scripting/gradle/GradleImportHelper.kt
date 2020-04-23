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
import com.intellij.openapi.util.Key
import com.intellij.testFramework.LightVirtualFileBase
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModelResolver
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.plugins.gradle.service.project.GradlePartialResolverPolicy
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverExtension
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.util.function.Predicate

fun runPartialGradleImport(project: Project) {
    getGradleProjectSettings(project).forEach {
        ExternalSystemUtil.refreshProject(
            it.externalProjectPath,
            ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
                .projectResolverPolicy(
                    GradlePartialResolverPolicy(Predicate<GradleProjectResolverExtension?> { it is KotlinDslScriptModelResolver })
                )
        )
    }
}

fun getMissingConfigurationNotificationText() = KotlinIdeaGradleBundle.message("script.configurations.will.be.available.after.load.changes")
fun getMissingConfigurationActionText() = KotlinIdeaGradleBundle.message("action.text.load.script.configurations")

fun showNotificationForProjectImport(project: Project) {}

fun hideNotificationForProjectImport(project: Project): Boolean = true

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

        e.presentation.description = KotlinIdeaGradleBundle.message("action.description.load.script.configurations")
    }

    private fun ensureValidActionVisibility(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        if (DiffUtil.isDiffEditor(editor)) {
            e.presentation.isVisible = false
            return
        }

        e.presentation.isVisible = editor.getNotificationVisibility()
    }

    private fun Editor.getNotificationVisibility(): Boolean {
        val project = project ?: return false

        val documentManager = FileDocumentManager.getInstance()
        val virtualFile = documentManager.getFile(document)
        if (virtualFile is LightVirtualFileBase) return false
        if (virtualFile == null || !virtualFile.isValid) return false

        return GradleScriptingSupportProvider.getInstance(project).isConfigurationOutOfDate(virtualFile)
    }
}
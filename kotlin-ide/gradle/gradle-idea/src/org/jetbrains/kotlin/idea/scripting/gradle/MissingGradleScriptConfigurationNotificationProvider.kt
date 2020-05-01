/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.icons.AllIcons
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.scripting.gradle.legacy.GradleLegacyScriptConfigurationLoaderForOutOfProjectScripts
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRoot
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

class MissingGradleScriptConfigurationNotificationProvider(private val project: Project) :
    EditorNotifications.Provider<EditorNotificationPanel>() {
    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (!isGradleKotlinScript(file)) return null
        if (file.fileType != KotlinFileType.INSTANCE) return null

        val scriptUnderRoot = GradleBuildRootsManager.getInstance(project).findScriptBuildRoot(file)
        val root = scriptUnderRoot?.root
        return when {
            root is GradleBuildRoot.Unlinked -> EditorNotificationPanel().apply {
                text(KotlinIdeaGradleBundle.message("text.the.associated.gradle.project.isn.t.imported"))

                val linkProjectText = KotlinIdeaGradleBundle.message("action.label.text.load.script.configuration")
                createActionLabel(linkProjectText) {
                    val newProjectSettings = GradleProjectSettings()
                    newProjectSettings.externalProjectPath = file.parent.path
                    ExternalSystemUtil.linkExternalProject(
                        GradleConstants.SYSTEM_ID,
                        newProjectSettings,
                        project,
                        {

                        },
                        false,
                        ProgressExecutionMode.IN_BACKGROUND_ASYNC
                    )
                }
                val link = createActionLabel("") {}
                link.setIcon(AllIcons.General.ContextHelp)
                link.setUseIconAsLink(true)
                link.toolTipText = KotlinIdeaGradleBundle.message(
                    "tool.tip.text.the.external.gradle.project.needs.to.be.imported.to.get.this.script.analyzed",
                    linkProjectText
                )
            }
            root is GradleBuildRoot.New && !root.importing -> EditorNotificationPanel().apply {
                text(getMissingConfigurationNotificationText())
                createActionLabel(getMissingConfigurationActionText()) {
                    runPartialGradleImport(project)
                }
            }
            else -> null
        }
    }

    private val loaderForOutOfProjectScripts by lazy {
        GradleLegacyScriptConfigurationLoaderForOutOfProjectScripts(
            project
        )
    }

    companion object {
        private val KEY = Key.create<EditorNotificationPanel>("GradleScriptOutOfSourceNotification")
    }
}

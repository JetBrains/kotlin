/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager

class MissingGradleScriptConfigurationNotificationProvider(private val project: Project) :
    EditorNotifications.Provider<EditorNotificationPanel>() {
    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (!isGradleKotlinScript(file)) return null
        if (file.fileType != KotlinFileType.INSTANCE) return null

        val scriptUnderRoot = GradleBuildRootsManager.getInstance(project).findScriptBuildRoot(file) ?: return null
        return when {
            scriptUnderRoot.isUnrelatedScript -> EditorNotificationPanel().apply {
                text(KotlinIdeaGradleBundle.message("text.the.associated.gradle.project.isn.t.imported"))

                createActionLabel(KotlinIdeaGradleBundle.message("action.text.standalone")) {
                    GradleBuildRootsManager.getInstance(project).addStandaloneScript(file)
                }

                val helpIcon = createActionLabel("") {}
                helpIcon.setIcon(AllIcons.General.ContextHelp)
                helpIcon.setUseIconAsLink(true)
                helpIcon.toolTipText = KotlinIdeaGradleBundle.message(
                    "tool.tip.text.the.external.gradle.project.needs.to.be.imported.to.get.this.script.analyzed"
                )
            }
            scriptUnderRoot.importRequired -> EditorNotificationPanel().apply {
                text(getMissingConfigurationNotificationText())
                createActionLabel(getMissingConfigurationActionText()) {
                    runPartialGradleImport(project)
                }
            }
            else -> null
        }
    }

    companion object {
        private val KEY = Key.create<EditorNotificationPanel>("GradleScriptOutOfSourceNotification")
    }
}

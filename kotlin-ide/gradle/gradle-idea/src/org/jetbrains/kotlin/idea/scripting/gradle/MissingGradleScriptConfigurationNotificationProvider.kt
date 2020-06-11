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
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsLocator.NotificationKind.*
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager

class MissingGradleScriptConfigurationNotificationProvider(private val project: Project) :
    EditorNotifications.Provider<EditorNotificationPanel>() {
    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (!isGradleKotlinScript(file)) return null
        if (file.fileType != KotlinFileType.INSTANCE) return null

        val rootsManager = GradleBuildRootsManager.getInstance(project)
        val scriptUnderRoot = rootsManager.findScriptBuildRoot(file) ?: return null
        return when (scriptUnderRoot.notificationKind) {
            dontCare -> null
            outsideAnything -> EditorNotificationPanel().apply {
                text(KotlinIdeaGradleBundle.message("notification.outsideAnything.text"))
                createActionLabel(KotlinIdeaGradleBundle.message("notification.outsideAnything.linkAction")) {
                    runPartialGradleImport(project)
                }
            }
            wasNotImportedAfterCreation -> EditorNotificationPanel().apply {
                text(KotlinIdeaGradleBundle.message("notification.wasNotImportedAfterCreation.text"))
                createActionLabel(getMissingConfigurationActionText()) {
                    runPartialGradleImport(project)
                }
            }
            notEvaluatedInLastImport -> EditorNotificationPanel().apply {
                text(KotlinIdeaGradleBundle.message(KotlinIdeaGradleBundle.message("notification.notEvaluatedInLastImport.text")))

                createActionLabel(KotlinIdeaGradleBundle.message("notification.notEvaluatedInLastImport.info")) {
                    rootsManager.updateStandaloneScripts {
                        addStandaloneScript(file.path)
                    }
                }

                contextHelp(KotlinIdeaGradleBundle.message("notification.notEvaluatedInLastImport.addAsStandaloneAction"))
            }
            standalone -> EditorNotificationPanel().apply {
                text(KotlinIdeaGradleBundle.message("notification.standalone.text"))
                createActionLabel(KotlinIdeaGradleBundle.message("notification.standalone.disableScriptAction")) {
                    rootsManager.updateStandaloneScripts {
                        removeStandaloneScript(file.path)
                    }
                }
                contextHelp(KotlinIdeaGradleBundle.message("notification.standalone.info"))
            }
        }
    }

    private fun EditorNotificationPanel.contextHelp(text: String) {
        val helpIcon = createActionLabel("") {}
        helpIcon.setIcon(AllIcons.General.ContextHelp)
        helpIcon.setUseIconAsLink(true)
        helpIcon.toolTipText = text
    }

    companion object {
        private val KEY = Key.create<EditorNotificationPanel>("GradleScriptOutOfSourceNotification")
    }
}

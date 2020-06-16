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
import org.jetbrains.kotlin.idea.core.util.KotlinIdeaCoreBundle
import org.jetbrains.kotlin.idea.scripting.gradle.legacy.GradleStandaloneScriptActions
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsLocator.NotificationKind.*
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager
import org.jetbrains.kotlin.idea.scripting.gradle.roots.Imported

class GradleScriptNotificationProvider(private val project: Project) :
    EditorNotifications.Provider<EditorNotificationPanel>() {
    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (!isGradleKotlinScript(file)) return null
        if (file.fileType != KotlinFileType.INSTANCE) return null

        val rootsManager = GradleBuildRootsManager.getInstance(project)
        val scriptUnderRoot = rootsManager.findScriptBuildRoot(file) ?: return null
        return when (scriptUnderRoot.notificationKind) {
            dontCare -> null
            legacyOutside -> EditorNotificationPanel().apply {
                text("Out of project script")
                createActionLabel(KotlinIdeaGradleBundle.message("notification.notEvaluatedInLastImport.addAsStandaloneAction")) {
                    rootsManager.updateStandaloneScripts {
                        addStandaloneScript(file.path)
                    }
                }
            }
            outsideAnything -> EditorNotificationPanel().apply {
                text(KotlinIdeaGradleBundle.message("notification.outsideAnything.text"))
                createActionLabel(KotlinIdeaGradleBundle.message("notification.outsideAnything.linkAction")) {
                    linkProject(project)
                }
            }
            wasNotImportedAfterCreation -> EditorNotificationPanel().apply {
                text(KotlinIdeaGradleBundle.message("notification.wasNotImportedAfterCreation.text"))
                createActionLabel(getMissingConfigurationActionText()) {
                    runPartialGradleImport(project)
                }
            }
            notEvaluatedInLastImport -> EditorNotificationPanel().apply {
                text(KotlinIdeaGradleBundle.message("notification.notEvaluatedInLastImport.text"))

                // suggest to reimport project if something changed after import
                val importTs = (scriptUnderRoot.nearest as? Imported)?.data?.importTs
                if (importTs != null && !scriptUnderRoot.nearest.areRelatedFilesChangedBefore(file, importTs)) {
                    createActionLabel(getMissingConfigurationActionText()) {
                        rootsManager.updateStandaloneScripts {
                            runPartialGradleImport(project)
                        }
                    }
                }

                // suggest to choose new gradle project
                createActionLabel(KotlinIdeaGradleBundle.message("notification.outsideAnything.linkAction")) {
                    linkProject(project)
                }

                createActionLabel(KotlinIdeaGradleBundle.message("notification.notEvaluatedInLastImport.addAsStandaloneAction")) {
                    rootsManager.updateStandaloneScripts {
                        addStandaloneScript(file.path)
                    }
                }

                contextHelp(KotlinIdeaGradleBundle.message("notification.notEvaluatedInLastImport.info"))
            }
            standalone -> EditorNotificationPanel().apply {
                val actions = GradleStandaloneScriptActions.getInstance(project).byFile[file]
                if (actions != null) {
                    text(
                        KotlinIdeaGradleBundle.message("notification.standalone.text") +
                                ". " +
                                KotlinIdeaCoreBundle.message("notification.text.script.configuration.has.been.changed")
                    )


                    createActionLabel(KotlinIdeaCoreBundle.message("notification.action.text.load.script.configuration")) {
                        actions.reload()
                    }

                    createActionLabel(KotlinIdeaCoreBundle.message("notification.action.text.enable.auto.reload")) {
                        actions.toggleAutoReload()
                    }
                } else {
                    text(KotlinIdeaGradleBundle.message("notification.standalone.text"))
                }

                createActionLabel(KotlinIdeaGradleBundle.message("notification.standalone.disableScriptAction")) {
                    rootsManager.updateStandaloneScripts {
                        removeStandaloneScript(file.path)
                    }
                }
                contextHelp(KotlinIdeaGradleBundle.message("notification.standalone.info"))
            }
        }
    }

    private fun linkProject(project: Project) {
        TODO()
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

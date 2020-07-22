/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.ImportModuleAction
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectImportProvider
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.core.util.KotlinIdeaCoreBundle
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.idea.scripting.gradle.legacy.GradleStandaloneScriptActionsManager
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsLocator
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsLocator.NotificationKind.*
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager
import org.jetbrains.kotlin.idea.scripting.gradle.roots.Imported
import java.io.File

class GradleScriptNotificationProvider(private val project: Project) :
    EditorNotifications.Provider<EditorNotificationPanel>() {
    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (!isGradleKotlinScript(file)) return null
        if (file.fileType != KotlinFileType.INSTANCE) return null

        val standaloneScriptActions = GradleStandaloneScriptActionsManager.getInstance(project)
        val rootsManager = GradleBuildRootsManager.getInstance(project)
        val scriptUnderRoot = rootsManager.findScriptBuildRoot(file) ?: return null

        // todo: this actions will be usefull only when gradle fix https://github.com/gradle/gradle/issues/12640
        fun EditorNotificationPanel.showActionsToFixNotEvaluated() {
            // suggest to reimport project if something changed after import
            val build: Imported = scriptUnderRoot.nearest as? Imported ?: return
            val importTs = build.data.importTs
            if (!build.areRelatedFilesChangedBefore(file, importTs)) {
                createActionLabel(getConfigurationsActionText()) {
                    rootsManager.updateStandaloneScripts {
                        runPartialGradleImport(project, build)
                    }
                }
            }

            // suggest to choose new gradle project
            createActionLabel(KotlinIdeaGradleBundle.message("notification.outsideAnything.linkAction")) {
                linkProject(project, scriptUnderRoot)
            }
        }

        return when (scriptUnderRoot.notificationKind) {
            dontCare -> null
            legacy -> {
                val actions = standaloneScriptActions[file]
                if (actions == null) null
                else {
                    object : EditorNotificationPanel() {
                        val contextHelp = KotlinIdeaGradleBundle.message("notification.gradle.legacy.firstLoad.info")

                        init {
                            if (actions.isFirstLoad) {
                                text(KotlinIdeaGradleBundle.message("notification.gradle.legacy.firstLoad"))
                                toolTipText = contextHelp
                            } else {
                                text(KotlinIdeaCoreBundle.message("notification.text.script.configuration.has.been.changed"))
                            }

                            createActionLabel(KotlinIdeaCoreBundle.message("notification.action.text.load.script.configuration")) {
                                actions.reload()
                            }

                            createActionLabel(KotlinIdeaCoreBundle.message("notification.action.text.enable.auto.reload")) {
                                actions.enableAutoReload()
                            }

                            if (actions.isFirstLoad) {
                                contextHelp(contextHelp)
                            }
                        }
                    }
                }
            }
            legacyOutside -> EditorNotificationPanel().apply {
                text(KotlinIdeaGradleBundle.message("notification.gradle.legacy.outsideProject"))
                createActionLabel(KotlinIdeaGradleBundle.message("notification.notEvaluatedInLastImport.addAsStandaloneAction")) {
                    rootsManager.updateStandaloneScripts {
                        addStandaloneScript(file.path)
                    }
                }
                contextHelp(KotlinIdeaGradleBundle.message("notification.gradle.legacy.outsideProject.addToStandaloneHelp"))
            }
            outsideAnything -> EditorNotificationPanel().apply {
                text(KotlinIdeaGradleBundle.message("notification.outsideAnything.text"))
                createActionLabel(KotlinIdeaGradleBundle.message("notification.outsideAnything.linkAction")) {
                    linkProject(project, scriptUnderRoot)
                }
            }
            wasNotImportedAfterCreation -> EditorNotificationPanel().apply {
                text(configurationsAreMissingRequestNeeded())
                createActionLabel(getConfigurationsActionText()) {
                    val root = scriptUnderRoot.nearest
                    if (root != null) {
                        runPartialGradleImport(project, root)
                    }
                }
                val help = configurationsAreMissingRequestNeededHelp()
                if (help != null) {
                    contextHelp(help)
                }
            }
            notEvaluatedInLastImport -> EditorNotificationPanel().apply {
                text(configurationsAreMissingAfterRequest())

                // todo: this actions will be usefull only when gradle fix https://github.com/gradle/gradle/issues/12640
                // showActionsToFixNotEvaluated()

                createActionLabel(KotlinIdeaGradleBundle.message("notification.notEvaluatedInLastImport.addAsStandaloneAction")) {
                    rootsManager.updateStandaloneScripts {
                        addStandaloneScript(file.path)
                    }
                }

                contextHelp(KotlinIdeaGradleBundle.message("notification.notEvaluatedInLastImport.info"))
            }
            standalone, standaloneLegacy -> EditorNotificationPanel().apply {
                val actions = standaloneScriptActions[file]
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
                        actions.enableAutoReload()
                    }
                } else {
                    text(KotlinIdeaGradleBundle.message("notification.standalone.text"))
                }

                createActionLabel(KotlinIdeaGradleBundle.message("notification.standalone.disableScriptAction")) {
                    rootsManager.updateStandaloneScripts {
                        removeStandaloneScript(file.path)
                    }
                }

                if (scriptUnderRoot.notificationKind == standaloneLegacy) {
                    contextHelp(KotlinIdeaGradleBundle.message("notification.gradle.legacy.standalone.info"))
                } else {
                    contextHelp(KotlinIdeaGradleBundle.message("notification.standalone.info"))
                }
            }
        }
    }

    private fun linkProject(
        project: Project,
        scriptUnderRoot: GradleBuildRootsLocator.ScriptUnderRoot
    ) {
        val settingsFile: File? = tryFindGradleSettings(scriptUnderRoot)

        // from AttachExternalProjectAction

        val manager = ExternalSystemApiUtil.getManager(GRADLE_SYSTEM_ID) ?: return
        val provider = ProjectImportProvider.PROJECT_IMPORT_PROVIDER.extensions.find {
            it is AbstractExternalProjectImportProvider && GRADLE_SYSTEM_ID == it.externalSystemId
        } ?: return
        val projectImportProviders = arrayOf(provider)

        if (settingsFile != null) {
            PropertiesComponent.getInstance().setValue(
                "last.imported.location",
                settingsFile.canonicalPath
            )
        }

        val wizard = ImportModuleAction.selectFileAndCreateWizard(
            project,
            null,
            manager.externalProjectDescriptor,
            projectImportProviders
        ) ?: return

        if (wizard.stepCount <= 0 || wizard.showAndGet()) {
            ImportModuleAction.createFromWizard(project, wizard)
        }
    }

    private fun tryFindGradleSettings(scriptUnderRoot: GradleBuildRootsLocator.ScriptUnderRoot): File? {
        try {
            var parent = File(scriptUnderRoot.filePath).canonicalFile.parentFile
            while (parent.isDirectory) {
                listOf("settings.gradle", "settings.gradle.kts").forEach {
                    val settings = parent.resolve(it)
                    if (settings.isFile) {
                        return settings
                    }
                }

                parent = parent.parentFile
            }
        } catch (t: Throwable) {
            // ignore
        }

        return null
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

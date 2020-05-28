/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.HyperlinkLabel
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.core.util.KotlinIdeaCoreBundle
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition

object LoadScriptConfigurationNotificationFactory {
    fun showNotification(file: VirtualFile, project: Project, onClick: () -> Unit): Boolean {
        file.addLoadConfigurationNotificationPanel(project, onClick)
        return true
    }

    fun hideNotification(file: VirtualFile, project: Project): Boolean {
        file.removeLoadConfigurationNotificationPanel(project)
        return true
    }


    private fun VirtualFile.removeLoadConfigurationNotificationPanel(project: Project) {
        withSelectedEditor(project) { manager ->
            notificationPanel?.let {
                manager.removeTopComponent(this, it)
            }
            notificationPanel = null
        }
    }

    private fun VirtualFile.addLoadConfigurationNotificationPanel(
        project: Project,
        onClick: () -> Unit
    ) {
        withSelectedEditor(project) { manager ->
            val existingPanel = notificationPanel
            if (existingPanel != null) {
                existingPanel.onClick = onClick
                return@withSelectedEditor
            }

            val panel = NewLoadConfigurationNotificationPanel(onClick, project, this@addLoadConfigurationNotificationPanel)
            notificationPanel = panel
            manager.addTopComponent(this, panel)
        }
    }

    private fun VirtualFile.withSelectedEditor(project: Project, f: FileEditor.(FileEditorManager) -> Unit) {
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater

            val fileEditorManager = FileEditorManager.getInstance(project)
            (fileEditorManager.getSelectedEditor(this))?.let {
                f(it, fileEditorManager)
            }
        }
    }

    private var FileEditor.notificationPanel: NewLoadConfigurationNotificationPanel?
            by UserDataProperty<FileEditor, NewLoadConfigurationNotificationPanel>(Key.create("load.script.configuration.panel"))

    private class NewLoadConfigurationNotificationPanel(
        var onClick: () -> Unit,
        project: Project,
        file: VirtualFile
    ) : EditorNotificationPanel() {

        init {
            setText(KotlinIdeaCoreBundle.message("notification.text.script.configuration.has.been.changed"))
            createComponentActionLabel(KotlinIdeaCoreBundle.message("notification.action.text.load.script.configuration")) {
                onClick()
            }

            createComponentActionLabel(KotlinIdeaCoreBundle.message("notification.action.text.enable.auto.reload")) {
                onClick()

                val scriptDefinition = file.findScriptDefinition(project) ?: return@createComponentActionLabel
                KotlinScriptingSettings.getInstance(project).setAutoReloadConfigurations(scriptDefinition, true)
            }
        }

        private fun EditorNotificationPanel.createComponentActionLabel(labelText: String, callback: (HyperlinkLabel) -> Unit) {
            val label: Ref<HyperlinkLabel> = Ref.create()
            val action = Runnable {
                callback(label.get())
            }
            label.set(createActionLabel(labelText, action))
        }
    }
}

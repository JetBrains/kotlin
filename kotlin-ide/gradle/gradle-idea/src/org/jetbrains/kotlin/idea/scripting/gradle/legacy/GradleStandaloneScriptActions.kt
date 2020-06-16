/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.legacy

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition

class GradleStandaloneScriptActions(val project: Project) {
    val byFile = mutableMapOf<VirtualFile, ForFile>()

    inner class ForFile(
        val file: VirtualFile,
        private val doLoad: () -> Unit
    ) {
        private val scriptDefinition
            get() = file.findScriptDefinition(project)

        val isAutoReloadAvailable: Boolean
        val isAutoReloadEnabled: Boolean

        init {
            val scriptDefinition = scriptDefinition
            if (scriptDefinition != null) {
                isAutoReloadAvailable = true
                isAutoReloadEnabled = KotlinScriptingSettings.getInstance(project)
                    .autoReloadConfigurations(scriptDefinition)
            } else {
                isAutoReloadAvailable = false
                isAutoReloadEnabled = false
            }
        }

        fun toggleAutoReload() {
            KotlinScriptingSettings.getInstance(project).setAutoReloadConfigurations(scriptDefinition!!, !isAutoReloadEnabled)
            updateNotification()
        }

        fun reload() {
            byFile.remove(file)
            doLoad()
            updateNotification()
        }

        fun updateNotification() {
            GradleBuildRootsManager.getInstance(project).updateNotifications(false) {
                it == file.path
            }
        }
    }

    companion object {
        fun getInstance(project: Project) =
            ServiceManager.getService(project, GradleStandaloneScriptActions::class.java)
    }
}
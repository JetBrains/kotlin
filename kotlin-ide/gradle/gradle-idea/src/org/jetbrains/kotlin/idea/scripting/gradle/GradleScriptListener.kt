/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangeListener
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager

class GradleScriptListener(project: Project) : ScriptChangeListener(project) {
    init {
        // start GradleScriptInputsWatcher to track changes in gradle-configuration related files
        project.service<GradleScriptInputsWatcher>().startWatching()
    }

    override fun isApplicable(vFile: VirtualFile) = GradleBuildRootsManager.getInstance(project).isApplicable(vFile)

    override fun editorActivated(vFile: VirtualFile) = checkUpToDate(vFile)

    override fun documentChanged(vFile: VirtualFile) = checkUpToDate(vFile)

    fun checkUpToDate(vFile: VirtualFile) {
        val upToDate = GradleBuildRootsManager.getInstance(project)
            .getScriptInfo(vFile)?.model?.inputs?.isUpToDate(project, vFile) ?: return

        if (upToDate) {
            hideNotificationForProjectImport(project)
        } else {
            showNotificationForProjectImport(project)
        }
    }
}
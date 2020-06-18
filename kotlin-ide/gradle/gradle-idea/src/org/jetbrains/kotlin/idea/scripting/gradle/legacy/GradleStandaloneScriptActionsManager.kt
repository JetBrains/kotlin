/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.legacy

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.TestOnly

class GradleStandaloneScriptActionsManager(val project: Project) {
    private val byFile = mutableMapOf<VirtualFile, GradleStandaloneScriptActions>()

    operator fun get(file: VirtualFile): GradleStandaloneScriptActions? = byFile[file]

    fun add(actionsProvider: (GradleStandaloneScriptActionsManager) -> GradleStandaloneScriptActions) {
        val actions = actionsProvider(this)
        byFile[actions.file] = actions
        actions.updateNotification()
    }

    fun remove(file: VirtualFile) {
        byFile.remove(file)?.updateNotification()
    }

    @TestOnly
    fun performSuggestedLoading(file: VirtualFile): Boolean {
        val actions = byFile[file] ?: return false
        actions.reload()
        return true
    }

    companion object {
        fun getInstance(project: Project) =
            ServiceManager.getService(project, GradleStandaloneScriptActionsManager::class.java)
    }
}
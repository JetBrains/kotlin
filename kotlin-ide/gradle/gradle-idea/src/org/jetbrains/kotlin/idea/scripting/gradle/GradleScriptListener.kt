/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.externalSystem.autoimport.AsyncFileChangeListenerBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.jetbrains.kotlin.idea.core.KotlinPluginDisposable
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangeListener
import org.jetbrains.kotlin.idea.scripting.gradle.legacy.GradleLegacyScriptListener
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager

class GradleScriptListener(project: Project) : ScriptChangeListener(project) {
    // todo(gradle6): remove
    private val legacy = GradleLegacyScriptListener(project)
    private val buildRootsManager = GradleBuildRootsManager.getInstance(project)

    init {
        // listen changes using VFS events, including gradle-configuration related files
        val listener = GradleScriptFileChangeListener(this, buildRootsManager)
        VirtualFileManager.getInstance().addAsyncFileListener(listener, KotlinPluginDisposable.getInstance(project))
    }

    fun fileChanged(filePath: String, ts: Long) =
        buildRootsManager.fileChanged(filePath, ts)

    override fun isApplicable(vFile: VirtualFile) =
        // todo(gradle6): replace with `isCustomScriptingSupport(vFile)`
        legacy.isApplicable(vFile)

    private fun isCustomScriptingSupport(vFile: VirtualFile) =
        buildRootsManager.isApplicable(vFile)

    override fun editorActivated(vFile: VirtualFile) {
        if (isCustomScriptingSupport(vFile)) {
            buildRootsManager.updateNotifications(restartAnalyzer = false) { it == vFile.path }
        } else {
            legacy.editorActivated(vFile)
        }
    }

    override fun documentChanged(vFile: VirtualFile) {
        fileChanged(vFile.path, System.currentTimeMillis())

        if (!isCustomScriptingSupport(vFile)) {
            legacy.documentChanged(vFile)
        }
    }
}

private class GradleScriptFileChangeListener(
    private val watcher: GradleScriptListener,
    private val buildRootsManager: GradleBuildRootsManager
) : AsyncFileChangeListenerBase() {
    val changedFiles = mutableListOf<String>()

    override fun init() {
        changedFiles.clear()
    }

    override fun isRelevant(path: String): Boolean {
        return buildRootsManager.maybeAffectedGradleProjectFile(path)
    }

    override fun updateFile(file: VirtualFile, event: VFileEvent) {
        changedFiles.add(event.path)
    }

    override fun apply() {
        changedFiles.forEach {
            LocalFileSystem.getInstance().findFileByPath(it)?.let { f ->
                watcher.fileChanged(f.path, f.timeStamp)
            }
        }
    }
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.externalSystem.autoimport.AsyncFileChangeListenerBase
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager

fun addVfsListener(
    watcher: GradleScriptListener,
    buildRootsManager: GradleBuildRootsManager
) {
    VirtualFileManager.getInstance().addAsyncFileListener(
        object : AsyncFileChangeListenerBase() {
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
        },
        watcher.project
    )
}

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.*

internal class FileSystemChangesTracker(project: Project) : Disposable {

    override fun dispose() {
        trackerListener.dispose()
    }

    private val trackerListener = object : VirtualFileListener, Disposable {

        val createdFilesMutable = mutableSetOf<VirtualFile>()

        @Volatile
        private var disposed = false

        init {
            VirtualFileManager.getInstance().addVirtualFileListener(this)
            Disposer.register(project, this)
        }

        @Synchronized
        override fun dispose() {
            if (!disposed) {
                disposed = true
                VirtualFileManager.getInstance().removeVirtualFileListener(this)
                createdFilesMutable.clear()
            }
        }

        override fun fileCreated(event: VirtualFileEvent) {
            createdFilesMutable.add(event.file)
        }

        override fun fileCopied(event: VirtualFileCopyEvent) {
            createdFilesMutable.add(event.file)
        }

        override fun fileMoved(event: VirtualFileMoveEvent) {
            createdFilesMutable.add(event.file)
        }
    }

    fun reset() {
        trackerListener.createdFilesMutable.clear()
    }

    val createdFiles: Set<VirtualFile> = trackerListener.createdFilesMutable
}
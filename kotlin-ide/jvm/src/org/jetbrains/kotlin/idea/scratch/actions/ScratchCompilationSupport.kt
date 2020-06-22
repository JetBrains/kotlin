/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.actions

import org.jetbrains.kotlin.idea.scratch.ScratchExecutor
import org.jetbrains.kotlin.idea.scratch.ScratchFile

object ScratchCompilationSupport {
    private data class FileExecutor(val file: ScratchFile, val executor: ScratchExecutor)
    @Volatile
    private var fileExecutor: FileExecutor? = null

    fun isInProgress(file: ScratchFile): Boolean = fileExecutor?.file == file
    fun isAnyInProgress(): Boolean = fileExecutor != null

    fun start(file: ScratchFile, executor: ScratchExecutor) {
        fileExecutor = FileExecutor(file, executor)
    }

    fun stop() {
        fileExecutor = null
    }

    fun forceStop() {
        fileExecutor?.executor?.stop()

        stop()
    }
}
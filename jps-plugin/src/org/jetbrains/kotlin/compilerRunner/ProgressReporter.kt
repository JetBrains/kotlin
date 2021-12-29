/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.messages.ProgressMessage

interface ProgressReporter {
    fun progress(message: String)
    fun compilationStarted()
    fun clearProgress()
}

class ProgressReporterImpl(private val context: CompileContext, private val chunk: ModuleChunk) : ProgressReporter {
    override fun progress(message: String) {
        context.processMessage(ProgressMessage("Kotlin: $message"))
    }

    override fun compilationStarted() {
        progress("compiling [${chunk.presentableShortName}]")
    }

    override fun clearProgress() {
        context.processMessage(ProgressMessage(""))
    }

}

inline fun <T> JpsCompilerEnvironment.withProgressReporter(fn: (ProgressReporter) -> T): T =
    try {
        fn(progressReporter)
    } finally {
        progressReporter.clearProgress()
    }
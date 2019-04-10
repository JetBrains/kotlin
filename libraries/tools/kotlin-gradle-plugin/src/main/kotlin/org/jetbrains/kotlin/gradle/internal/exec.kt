/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.process.internal.ExecAction
import org.gradle.process.internal.ExecActionFactory
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

internal fun Project.execWithProgress(description: String, body: (ExecAction) -> Unit) {
    this as ProjectInternal

    val stderr = ByteArrayOutputStream()
    val stdout = StringBuilder()
    val stdInPipe = PipedInputStream()
    val exec = services.get(ExecActionFactory::class.java).newExecAction()
    body(exec)
    project.operation(description) {
        exec.errorOutput = stderr
        exec.standardOutput = PipedOutputStream(stdInPipe)
        val outputReaderThread = thread(name = "output reader for [$description]") {
            stdInPipe.reader().useLines { lines ->
                lines.forEach {
                    stdout.appendln(it)
                    progress(it)
                }
            }
        }
        exec.isIgnoreExitValue = true
        val result = exec.execute()
        outputReaderThread.join()
        if (result.exitValue != 0) {
            error(stderr.toString() + "\n" + stdout)
        }
    }
}
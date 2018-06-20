/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.gradle.utils.pathsAsStringRelativeTo
import org.jetbrains.kotlin.incremental.ICReporter
import java.io.File

internal class GradleICReporter(private val projectRootFile: File) : ICReporter {
    private val log = Logging.getLogger(GradleICReporter::class.java)

    override fun report(message: () -> String) {
        log.kotlinDebug(message)
    }

    override fun pathsAsString(files: Iterable<File>): String =
        files.pathsAsStringRelativeTo(projectRootFile)

    override fun reportCompileIteration(sourceFiles: Collection<File>, exitCode: ExitCode) {
        if (sourceFiles.any()) {
            report { "compile iteration: ${pathsAsString(sourceFiles)}" }
        }
        report { "compiler exit code: $exitCode" }
    }
}


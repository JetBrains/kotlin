/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.incremental

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import java.io.File

internal class GradleICReporter(private val projectRootFile: File) : ICReporter {
    private val log = Logging.getLogger(GradleICReporter::class.java)

    override fun report(message: ()->String) {
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


/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.konan.util

import org.jetbrains.kotlin.konan.file.unzipTo
import java.io.File
import java.util.concurrent.TimeUnit


class DependencyExtractor {
    internal val useZip = System.getProperty("os.name").startsWith("Windows")

    internal val archiveExtension = if (useZip) {
        "zip"
    } else {
        "tar.gz"
    }

    private fun extractTarGz(tarGz: File, targetDirectory: File) {
        val tarProcess = ProcessBuilder().apply {
            command("tar", "-xzf", tarGz.canonicalPath)
            directory(targetDirectory)
            inheritIO()
        }.start()
        val finished = tarProcess.waitFor(extractionTimeout, extractionTimeoutUntis)
        when {
            finished && tarProcess.exitValue() != 0 ->
                throw RuntimeException(
                    "Cannot extract archive with dependency: ${tarGz.canonicalPath}.\n" +
                    "Tar exit code: ${tarProcess.exitValue()}."
                )
            !finished -> {
                tarProcess.destroy()
                throw RuntimeException(
                    "Cannot extract archive with dependency: ${tarGz.canonicalPath}.\n" +
                    "Tar process hasn't finished in ${extractionTimeoutUntis.toSeconds(extractionTimeout)} sec.")
            }
        }
    }

    fun extract(archive: File, targetDirectory: File) {
        if (useZip) {
            archive.toPath().unzipTo(targetDirectory.toPath())
        } else {
            extractTarGz(archive, targetDirectory)
        }
    }

    companion object {
        val extractionTimeout = 3600L
        val extractionTimeoutUntis = TimeUnit.SECONDS
    }

}
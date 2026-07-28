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

import org.jetbrains.kotlin.io.unzipTo
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

enum class ArchiveType(val fileExtension: String) {
    ZIP("zip"),
    TAR_GZ("tar.gz"),

    /** Apple's signed `.xip` archive, expanded with `/usr/bin/xip`. macOS only. */
    XIP("xip");

    companion object {
        val systemDefault = if (System.getProperty("os.name").startsWith("Windows")) {
            ZIP
        } else {
            TAR_GZ
        }
    }
}

class DependencyExtractor : ArchiveExtractor {

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
                            "Tar process hasn't finished in ${extractionTimeoutUntis.toSeconds(extractionTimeout)} sec."
                )
            }
        }
    }

    // `xip --expand` produces the bundle (e.g. `Xcode.app`) with its own name, not one matching the dependency. It is
    // expanded into a temp dir, then the single produced bundle is renamed to the dependency directory (the archive is
    // cached as `<dependency>.xip`, so its base name is exactly the directory name callers expect).
    private fun extractXip(xip: File, targetDirectory: File) {
        val targetName = xip.name.removeSuffix(".${ArchiveType.XIP.fileExtension}")
        val expandDir = Files.createTempDirectory(targetDirectory.toPath(), "xip-expand-").toFile()
        try {
            val process = ProcessBuilder().apply {
                command("/usr/bin/xip", "--expand", xip.canonicalPath)
                directory(expandDir)
                inheritIO()
            }.start()
            val finished = process.waitFor(extractionTimeout, extractionTimeoutUntis)
            when {
                finished && process.exitValue() != 0 ->
                    throw RuntimeException(
                        "Cannot expand archive with dependency: ${xip.canonicalPath}.\n" +
                                "xip exit code: ${process.exitValue()}."
                    )
                !finished -> {
                    process.destroy()
                    throw RuntimeException(
                        "Cannot expand archive with dependency: ${xip.canonicalPath}.\n" +
                                "xip process hasn't finished in ${extractionTimeoutUntis.toSeconds(extractionTimeout)} sec."
                    )
                }
            }
            val bundle = expandDir.listFiles()?.firstOrNull { it.name.endsWith(".app") }
                ?: error("Expanding ${xip.canonicalPath} did not produce an .app bundle.")
            // `xcrun`/`xcode-select` reject a developer dir unless its enclosing bundle is named `*.app`. The
            // dependency directory itself must stay `xcode_<version>_<build>` (no `.app`) — that is the name CI agent
            // images pre-create and the compiler resolves as `DEVELOPER_DIR`. So the real bundle is stored as
            // `<name>.app` and the dependency name is exposed as a symlink to it, giving the exact same shape as a
            // symlinked current Xcode. DependencyProcessor's idempotency check (`exists`/`isDirectory`/`list`) and
            // `xcrun` both follow the symlink to the `.app` bundle.
            val appBundle = File(targetDirectory, "$targetName.app")
            appBundle.deleteRecursively()
            check(bundle.renameTo(appBundle)) {
                "Failed to move ${bundle.canonicalPath} to ${appBundle.canonicalPath}."
            }
            val dependencyLink = File(targetDirectory, targetName)
            if (Files.isSymbolicLink(dependencyLink.toPath())) Files.delete(dependencyLink.toPath()) else dependencyLink.deleteRecursively()
            Files.createSymbolicLink(dependencyLink.toPath(), Paths.get("$targetName.app"))
        } finally {
            expandDir.deleteRecursively()
        }
    }

    override fun extract(archive: File, targetDirectory: File, archiveType: ArchiveType) {
        when (archiveType) {
            ArchiveType.ZIP -> archive.toPath().unzipTo(targetDirectory.toPath())
            ArchiveType.TAR_GZ -> extractTarGz(archive, targetDirectory)
            ArchiveType.XIP -> extractXip(archive, targetDirectory)
        }
    }

    companion object {
        val extractionTimeout = 3600L
        val extractionTimeoutUntis = TimeUnit.SECONDS
    }

}

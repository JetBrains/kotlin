/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.*
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.*

internal class TaskOutputsBackup(
    val fileSystemOperations: FileSystemOperations,
    val buildDirectory: DirectoryProperty,
    val snapshotsDir: Provider<Directory>,

    allOutputs: List<File>,

    /**
     * Task outputs that we don't want to back up for performance reasons (e.g., if (1) they are too big, and (2) they are usually updated
     * only at the end of the task execution--in a failed task run, they are usually unchanged and therefore don't need to be restored).
     *
     * NOTE: In `IncrementalCompilerRunner`, if incremental compilation fails, it will try again by cleaning all the outputs and perform
     * non-incremental compilation. It is important that `IncrementalCompilerRunner` do not clean [outputsToExclude] immediately but only
     * right before [outputsToExclude] are updated (which is usually at the end of the task execution). This is so that if the fallback
     * compilation fails, [outputsToExclude] will remain unchanged and the other outputs will be restored, and the next task run can be
     * incremental.
     */
    outputsToExclude: List<File> = emptyList(),
    val logger: Logger
) {
    /** The outputs to back up and restore. Note that this may be a subset of all the outputs of a task (see `outputsToExclude`). */
    val outputs: List<File> = allOutputs - outputsToExclude.toSet()

    fun createSnapshot() {
        // Kotlin JS compilation task declares one file from 'destinationDirectory' output as task `@OutputFile'
        // property. To avoid snapshot sync collisions, each snapshot output directory has also 'index' as prefix.
        outputs.toSortedSet().forEachIndexed { index, outputPath ->
            if (outputPath.isDirectory && !outputPath.isEmptyDirectory) {
                compressDirectoryToZip(
                    File(snapshotsDir.get().asFile, index.asSnapshotArchiveName),
                    outputPath
                )
            } else {
                fileSystemOperations.copy { spec ->
                    spec.from(outputPath)
                    spec.into(snapshotsDir.map { it.file(index.asSnapshotDirectoryName).asFile })
                }
            }
        }
    }

    fun restoreOutputs() {
        fileSystemOperations.delete {
            it.delete(outputs)
        }

        outputs.toSortedSet().forEachIndexed { index, outputPath ->
            val snapshotDir = snapshotsDir.get().file(index.asSnapshotDirectoryName).asFile
            if (snapshotDir.isDirectory) {
                fileSystemOperations.copy { spec ->
                    spec.from(snapshotDir)
                    spec.into(outputPath.parentFile)
                }
            } else {
                val snapshotArchive = snapshotsDir.get().file(index.asSnapshotArchiveName).asFile
                if (!snapshotArchive.exists()) {
                    logger.warn(
                        """
                        |Failed to restore task outputs as snapshot file ${snapshotArchive.absolutePath} does not exist!
                        |On recompilation full rebuild will be performed.
                        """.trimMargin()
                    )
                    return
                }
                uncompressZipIntoDirectory(snapshotArchive, outputPath)
            }
        }
    }

    fun deleteSnapshot() {
        fileSystemOperations.delete { it.delete(snapshotsDir) }
    }

    /**
     * Kotlin's compilation in a "fat" project may contain a lot of small files that is slow to copy
     * So we speeding it up by archiving them into single zip file without compression. Such approach reduces snapshotting
     * time up to half ot the time needed to copy similar files.
     */
    private fun compressDirectoryToZip(
        snapshotFile: File,
        outputPath: File
    ) {
        snapshotFile.parentFile.mkdirs()
        snapshotFile.createNewFile()

        ZipOutputStream(snapshotFile.outputStream().buffered()).use { zip ->
            zip.setLevel(Deflater.NO_COMPRESSION)
            outputPath
                .walkTopDown()
                .filter { file -> !file.isDirectory || file.isEmptyDirectory }
                .forEach { file ->
                    val suffix = if (file.isDirectory) "/" else ""
                    val entry = ZipEntry(file.relativeTo(outputPath).invariantSeparatorsPath + suffix)
                    zip.putNextEntry(entry)
                    if (!file.isDirectory) {
                        file.inputStream().buffered().use { it.copyTo(zip) }
                    }
                    zip.closeEntry()
                }
            zip.flush()
        }
    }

    private fun uncompressZipIntoDirectory(
        snapshotFile: File,
        outputDirectory: File
    ) {
        val outputPath = outputDirectory.toPath()
        val snapshotUri = URI.create("jar:${snapshotFile.toURI()}")
        FileSystems.newFileSystem(snapshotUri, emptyMap<String, Any>()).use { zipFs ->
            zipFs.rootDirectories.forEach { rootDir ->
                Files.walk(rootDir).use { paths ->
                    paths.forEach {
                        if (Files.isDirectory(it)) {
                            Files.createDirectories(outputPath.resolve(it.normalizedToBeRelative))
                        } else if (Files.isRegularFile(it)) {
                            Files.copy(it, outputPath.resolve(it.normalizedToBeRelative), StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                }
            }
        }
    }

    private val File.isEmptyDirectory: Boolean
        get() = !Files.list(toPath()).use { it.findFirst().isPresent }

    private val Path.normalizedToBeRelative: String
        get() = if (toString() == "/") "." else toString().removePrefix("/")

    private val Int.asSnapshotArchiveName: String
        get() = "$this.zip"

    private val Int.asSnapshotDirectoryName: String
        get() = "$this"
}

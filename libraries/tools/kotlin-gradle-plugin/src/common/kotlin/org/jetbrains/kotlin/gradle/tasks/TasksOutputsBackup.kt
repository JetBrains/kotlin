/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class TaskOutputsBackup(
    private val fileSystemOperations: FileSystemOperations,
    val buildDirectory: DirectoryProperty,
    val snapshotsDir: Provider<Directory>,

    /**
     * Task outputs to back up and restore.
     *
     * Note that this could be a subset of all the outputs of a task because there could be task outputs that we don't want to back up and
     * restore (e.g., if (1) they are too big and (2) they are updated only at the end of the task execution so in a failed task run, they
     * are usually unchanged and therefore don't need to be restored).
     */
    val outputsToRestore: List<File>,

    val logger: Logger
) {

    fun createSnapshot() {
        // Kotlin JS compilation task declares one file from 'destinationDirectory' output as task `@OutputFile'
        // property. To avoid snapshot sync collisions, each snapshot output directory has also 'index' as prefix.
        outputsToRestore.toSortedSet().forEachIndexed { index, outputPath ->
            if (outputPath.isDirectory && !outputPath.isEmptyDirectory) {
                val snapshotFile = File(snapshotsDir.get().asFile, index.asSnapshotArchiveName)
                logger.debug("Packing $outputPath as $snapshotFile to make a backup")
                compressDirectoryToZip(
                    snapshotFile,
                    outputPath
                )
            } else if (!outputPath.exists()) {
                logger.debug("Ignoring $outputPath in making a backup as it does not exist")
                val markerFile = File(snapshotsDir.get().asFile, index.asNotExistsMarkerFile)
                markerFile.parentFile.mkdirs()
                markerFile.createNewFile()
            } else {
                val snapshotFile = snapshotsDir.map { it.file(index.asSnapshotDirectoryName).asFile }
                logger.debug("Copying $outputPath as $snapshotFile to make a backup")
                fileSystemOperations.copy { spec ->
                    spec.from(outputPath)
                    spec.into(snapshotFile)
                }
            }
        }
    }

    fun restoreOutputs() {
        fileSystemOperations.delete {
            it.delete(outputsToRestore)
        }

        outputsToRestore.toSortedSet().forEachIndexed { index, outputPath ->
            val snapshotDir = snapshotsDir.get().file(index.asSnapshotDirectoryName).asFile
            if (snapshotDir.isDirectory) {
                logger.debug("Copying files from $snapshotDir into ${outputPath.parentFile} to restore from backup")
                fileSystemOperations.copy { spec ->
                    spec.from(snapshotDir)
                    spec.into(outputPath.parentFile)
                }
            } else if (snapshotsDir.get().file(index.asNotExistsMarkerFile).asFile.exists()) {
                // do nothing
            } else {
                val snapshotArchive = snapshotsDir.get().file(index.asSnapshotArchiveName).asFile
                logger.debug("Unpacking $snapshotArchive into $outputPath to restore from backup")
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

    private val Int.asNotExistsMarkerFile: String
        get() = "$this.not-exists"

    private val Int.asSnapshotDirectoryName: String
        get() = "$this"
}

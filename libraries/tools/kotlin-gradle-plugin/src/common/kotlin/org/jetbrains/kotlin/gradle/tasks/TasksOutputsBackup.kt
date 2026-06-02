/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.invariantSeparatorsPathString
import org.jetbrains.kotlin.gradle.utils.use
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
    val snapshotsDir: Provider<Directory>,

    /**
     * Task outputs to back up and restore.
     *
     * Note that this could be a subset of all the outputs of a task because there could be task outputs that we don't want to back up and
     * restore (e.g., if (1) they are too big and (2) they are updated only at the end of the task execution so in a failed task run, they
     * are usually unchanged and therefore don't need to be restored).
     */
    val outputsToRestore: List<File>,

    val logger: KotlinLogger,
) {

    fun createSnapshot() {
        // Kotlin JS compilation task declares one file from 'destinationDirectory' output as task `@OutputFile'
        // property. To avoid snapshot sync collisions, each snapshot output directory has also 'index' as prefix.
        outputsToRestore.toSortedSet().forEachIndexed { index, outputPath ->
            if (outputPath.isDirectory) {
                val snapshotFile = snapshotsDir.get().asFile.toPath().resolve(index.asSnapshotArchiveName)
                logger.debug("Packing ${outputPath.invariantSeparatorsPath} as ${snapshotFile.invariantSeparatorsPathString} to make a backup")
                compressDirectoryToZip(
                    snapshotFile,
                    outputPath.toPath()
                )
            } else if (!outputPath.exists()) {
                logger.debug("Creating is-empty marker file for ${outputPath.invariantSeparatorsPath} as it does not exist")
                val markerFile = snapshotsDir.get().asFile.toPath().resolve(index.asNotExistsMarkerFile)
                Files.createDirectories(markerFile.parent)
                if (!Files.exists(markerFile)) {
                    Files.createFile(markerFile)
                }
            } else { // it's not a directory, but it exists -> it's a file
                val personalSnapshotDir = snapshotsDir.map { it.file(index.asSnapshotHolderDirectory).asFile }.get()
                logger.debug("Copying ${outputPath.invariantSeparatorsPath} into ${personalSnapshotDir.invariantSeparatorsPath} to make a backup")
                fileSystemOperations.copy { spec ->
                    spec.from(outputPath)
                    spec.into(personalSnapshotDir)
                }
            }
        }
    }

    fun restoreOutputs() {
        fileSystemOperations.delete {
            it.delete(outputsToRestore)
        }

        outputsToRestore.toSortedSet().forEachIndexed { index, outputPath ->
            val possibleDir = snapshotsDir.get().file(index.asSnapshotHolderDirectory).asFile.toPath()
            val possibleArchive = snapshotsDir.get().file(index.asSnapshotArchiveName).asFile.toPath()
            val possibleNotExistsMarker = snapshotsDir.get().file(index.asNotExistsMarkerFile).asFile.toPath()

            if (Files.exists(possibleArchive)) {
                logger.debug("Unpacking ${possibleArchive.invariantSeparatorsPathString} into ${outputPath.invariantSeparatorsPath} to restore from backup")
                Files.createDirectories(outputPath.toPath())
                uncompressZipIntoDirectory(possibleArchive, outputPath.toPath())
            } else if (Files.exists(possibleDir)) {
                logger.debug("Copying file from ${possibleDir.invariantSeparatorsPathString} into ${outputPath.parentFile.invariantSeparatorsPath} to restore ${outputPath.name} from backup")
                fileSystemOperations.copy { spec ->
                    spec.from(possibleDir.toFile())
                    spec.into(outputPath.parentFile)
                }
            } else if (Files.exists(possibleNotExistsMarker)) {
                // do nothing
                logger.debug("Found marker ${possibleNotExistsMarker.invariantSeparatorsPathString} for ${outputPath.invariantSeparatorsPath}, doing nothing")
            } else {
                logger.warn(
                    """
                    |Failed to restore task outputs as all possible snapshot files for ${outputPath.invariantSeparatorsPath} do not exist!
                    |On recompilation full rebuild will be performed.
                    """.trimMargin()
                )
                val walkSnapshots = {
                    Files.walk(snapshotsDir.getFile().toPath())
                        .map {
                            it.invariantSeparatorsPathString
                        }
                        .iterator()
                        .asSequence().toList().joinToString(",")
                }
                logger.debug("Available snapshots: ${walkSnapshots()}")
                return
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
        snapshotFile: Path,
        outputPath: Path
    ) {
        Files.createDirectories(snapshotFile.parent)
        if (!Files.exists(snapshotFile)) {
            Files.createFile(snapshotFile)
        }

        ZipOutputStream(Files.newOutputStream(snapshotFile).buffered()).use { zip ->
            zip.setLevel(Deflater.NO_COMPRESSION)
            Files.walk(outputPath).use { paths ->
                paths
                    .filter { file -> !Files.isDirectory(file) || file.isEmptyDirectory }
                    .forEach { file ->
                        val suffix = if (Files.isDirectory(file)) "/" else ""
                        val entry = ZipEntry(outputPath.relativize(file).invariantSeparatorsPathString + suffix)
                        zip.putNextEntry(entry)
                        if (!Files.isDirectory(file)) {
                            Files.newInputStream(file).buffered().use { it.copyTo(zip) }
                        }
                        zip.closeEntry()
                    }
            }
            zip.flush()
        }
    }

    private fun uncompressZipIntoDirectory(
        snapshotFile: Path,
        outputPath: Path
    ) {
        val snapshotUri = URI.create("jar:${snapshotFile.toUri()}")
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

    private val Path.isEmptyDirectory: Boolean
        get() = !Files.list(this).use { it.findFirst().isPresent }

    private val Path.normalizedToBeRelative: String
        get() = if (toString() == "/") "." else toString().removePrefix("/")

    private val Int.asSnapshotArchiveName: String
        get() = "$this.zip"

    private val Int.asNotExistsMarkerFile: String
        get() = "$this.not-exists"

    private val Int.asSnapshotHolderDirectory: String
        get() = "$this"
}

internal fun interface BackupRestoreWrapper {
    fun wrap(restoreAction: () -> Unit)
}

internal fun TaskOutputsBackup.tryRestoringOnRecoverableException(
    e: FailedCompilationException,
    restoreWrapper: BackupRestoreWrapper,
) {
    // Restore outputs only in cases where we expect that the user will make some changes to their project:
    //   - For a compilation error, the user will need to fix their source code
    //   - For an OOM error, the user will need to increase their memory settings
    // In the other cases where there is nothing the user can fix in their project, we should not restore the outputs.
    // Otherwise, the next build(s) will likely fail in exactly the same way as this build because their inputs and outputs are
    // the same.
    if (e is CompilationErrorException || e is OOMErrorException) {
        restoreWrapper.wrap {
            restoreOutputs()
        }
    }
}

internal const val DEFAULT_BACKUP_RESTORE_MESSAGE = "Restoring task outputs to pre-compilation state"

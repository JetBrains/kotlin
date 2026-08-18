/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.IOException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.GZIPInputStream
import kotlin.io.path.inputStream

internal fun Path.unzipTarGz(destinationDirectory: Path) {
    val targetBase = destinationDirectory.normalize().toAbsolutePath()
    createDirectoriesIfMissing(targetBase)
    // Resolved once here (targetBase exists by now) and reused for every real-path check below.
    val realTargetBase = targetBase.toRealPath()

    GZIPInputStream(BufferedInputStream(inputStream())).use { gzipInputStream ->
        val hardLinks = mutableMapOf<Path, Path>()

        TarArchiveInputStream(gzipInputStream).use { tarInputStream ->
            generateSequence { tarInputStream.nextEntry }
                .forEach { entry: TarArchiveEntry ->
                    // CWE-22: reject ../.. in entry names
                    val outputPath = targetBase.resolve(entry.name).normalize()
                    requireInsideBase(targetBase, outputPath, "Entry '${entry.name}'")

                    when {
                        entry.isDirectory -> {
                            requireRealPathInsideTarget(realTargetBase, outputPath, "Directory '${entry.name}'")
                            createDirectoriesIfMissing(outputPath)
                        }
                        entry.isSymbolicLink -> {
                            // Escaping symlinks are allowed: xcode-addon bundles point outside the root
                            // by design, and creating one writes nothing through it. A later write
                            // through it is rejected below. Only check the link's own location here.
                            requireRealPathInsideTarget(realTargetBase, outputPath, "Symlink '${entry.name}'")
                            createDirectoriesIfMissing(outputPath.parent)
                            deleteExistingEntry(outputPath, "Symlink '${entry.name}'")
                            Files.createSymbolicLink(outputPath, Paths.get(entry.linkName))
                        }
                        entry.isLink -> {
                            // Hardlink targets are relative to the archive root. Check they stay inside;
                            // the real-path check waits until every symlink exists (see below).
                            val hardlinkTarget = targetBase.resolve(entry.linkName).normalize()
                            requireInsideBase(targetBase, hardlinkTarget, "Hardlink target '${entry.linkName}'")
                            hardLinks[outputPath] = hardlinkTarget
                        }
                        else -> {
                            requireRealPathInsideTarget(realTargetBase, outputPath, "File '${entry.name}'")
                            createDirectoriesIfMissing(outputPath.parent)
                            // The leaf is not real-path checked, so it may be a symlink from an earlier entry.
                            // Unlink it first so we don't write through it.
                            deleteExistingEntry(outputPath, "File '${entry.name}'")
                            Files.newOutputStream(outputPath).use { tarInputStream.copyTo(it) }
                            if (supportsPosixFilePermissions) {
                                Files.setPosixFilePermissions(outputPath, getPosixFilePermissions(entry.mode))
                            }
                        }
                    }
                }
        }
        hardLinks.forEach { (linkPath, targetPath) ->
            requireRealPathInsideTarget(realTargetBase, linkPath, "Hardlink '${linkPath.fileName}'")
            requireRealPathInsideTarget(realTargetBase, targetPath, "Hardlink target '${targetPath.fileName}'")
            createDirectoriesIfMissing(linkPath.parent)
            deleteExistingEntry(linkPath, "Hardlink '${linkPath.fileName}'")
            Files.createLink(linkPath, targetPath)
        }
    }
}

internal class TarExtractionSecurityException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

private val supportsPosixFilePermissions: Boolean by lazy {
    FileSystems.getDefault().supportedFileAttributeViews().contains("posix")
}

private fun createDirectoriesIfMissing(dir: Path?) {
    if (dir != null && !Files.isDirectory(dir)) {
        Files.createDirectories(dir)
    }
}

private fun requireInsideBase(base: Path, candidate: Path, description: String) {
    if (!candidate.startsWith(base)) {
        throw TarExtractionSecurityException("$description escapes target directory")
    }
}

/**
 * Rejects writing through an extracted symlink that points outside [realTargetBase] (CWE-59).
 * Finds the deepest existing ancestor of [outputPath], resolves it with toRealPath (which
 * follows symlink chains), and requires the result to stay inside [realTargetBase].
 *
 * Never called on a symlink's target, so outward-pointing symlinks are still allowed.
 */
private fun requireRealPathInsideTarget(realTargetBase: Path, outputPath: Path, description: String) {
    var ancestor: Path? = outputPath.parent
    while (ancestor != null && !Files.exists(ancestor, LinkOption.NOFOLLOW_LINKS)) {
        ancestor = ancestor.parent
    }
    // Nothing exists yet, so no symlink can redirect us.
    if (ancestor == null) return
    val realAncestor = try {
        ancestor.toRealPath()
    } catch (e: IOException) {
        // Dangling symlink, permission problem, etc. -- we can't prove containment, so refuse.
        throw TarExtractionSecurityException("$description cannot be safely resolved ('$ancestor')", e)
    }
    if (!realAncestor.startsWith(realTargetBase)) {
        throw TarExtractionSecurityException("$description writes through an escaping symlink")
    }
}

// Unlinks whatever occupies [path] before creating a new entry, like tar does.
// Refuses to delete a non-empty directory: that means the archive conflicts
// with itself or with previously extracted content.
private fun deleteExistingEntry(path: Path, description: String) {
    try {
        Files.deleteIfExists(path)
    } catch (e: DirectoryNotEmptyException) {
        throw TarExtractionSecurityException("$description conflicts with an existing non-empty directory at '$path'", e)
    }
}

private fun getPosixFilePermissions(mode: Int): Set<PosixFilePermission> = buildSet {
    // adding owner permissions
    addPermission(mode, 0b100_000_000, PosixFilePermission.OWNER_READ)
    addPermission(mode, 0b010_000_000, PosixFilePermission.OWNER_WRITE)
    addPermission(mode, 0b001_000_000, PosixFilePermission.OWNER_EXECUTE)

    // adding group permissions
    addPermission(mode, 0b000_100_000, PosixFilePermission.GROUP_READ)
    addPermission(mode, 0b000_010_000, PosixFilePermission.GROUP_WRITE)
    addPermission(mode, 0b000_001_000, PosixFilePermission.GROUP_EXECUTE)

    // adding other permissions
    addPermission(mode, 0b000_000_100, PosixFilePermission.OTHERS_READ)
    addPermission(mode, 0b000_000_010, PosixFilePermission.OTHERS_WRITE)
    addPermission(mode, 0b000_000_001, PosixFilePermission.OTHERS_EXECUTE)
}

private fun MutableSet<PosixFilePermission>.addPermission(mode: Int, permissionBitMask: Int, permission: PosixFilePermission) {
    if ((mode and permissionBitMask) != 0) {
        add(permission)
    }
}

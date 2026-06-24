/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.GZIPInputStream
import kotlin.io.path.inputStream
import kotlin.io.use

internal fun Path.unzipTarGz(destinationDirectory: Path) {
    val targetBase = destinationDirectory.normalize().toAbsolutePath()
    Files.createDirectories(targetBase)
    val targetBaseReal = targetBase.toRealPath()
    val verifiedAncestors = HashSet<Path>()

    GZIPInputStream(BufferedInputStream(inputStream())).use { gzipInputStream ->
        val hardLinks = HashMap<Path, Path>()

        TarArchiveInputStream(gzipInputStream).use { tarInputStream ->
            generateSequence {
                tarInputStream.nextEntry
            }.forEach { entry: TarArchiveEntry ->
                val outputPath = validateOutputPath(targetBase, entry.name)
                if (entry.isDirectory) {
                    requireRealPathInsideTarget(targetBase, targetBaseReal, outputPath, verifiedAncestors)
                    Files.createDirectories(outputPath)
                } else {
                    if (entry.isSymbolicLink) {
                        // The link may point outside targetBase (e.g. xcode-addon), but it must
                        // be created inside it, not through an escaping ancestor symlink.
                        requireRealPathInsideTarget(targetBase, targetBaseReal, outputPath, verifiedAncestors)
                        Files.createDirectories(outputPath.parent)
                        Files.createSymbolicLink(outputPath, Paths.get(entry.linkName))
                    } else if (entry.isLink) {
                        hardLinks[outputPath] = validateHardlinkTarget(targetBase, entry.linkName)
                    } else {
                        requireRealPathInsideTarget(targetBase, targetBaseReal, outputPath, verifiedAncestors)
                        Files.createDirectories(outputPath.parent)
                        Files.newOutputStream(outputPath).use {
                            tarInputStream.copyTo(it)
                        }
                        if (supportsPosixFilePermissions) {
                            Files.setPosixFilePermissions(outputPath, getPosixFilePermissions(entry.mode))
                        }
                    }
                }
            }
        }
        hardLinks.forEach { (linkPath, targetPath) ->
            requireRealPathInsideTarget(targetBase, targetBaseReal, linkPath, verifiedAncestors)
            requireRealPathInsideTarget(targetBase, targetBaseReal, targetPath, verifiedAncestors)
            Files.createLink(linkPath, targetPath)
        }
    }
}

internal class TarExtractionSecurityException(message: String) : IOException(message)

private val supportsPosixFilePermissions: Boolean by lazy {
    FileSystems.getDefault().supportedFileAttributeViews().contains("posix")
}

private fun validateOutputPath(targetBase: Path, entryName: String): Path {
    val outputPath = targetBase.resolve(entryName).normalize()
    requireInsideTarget(targetBase, outputPath, "Entry '$entryName'")
    return outputPath
}

/**
 * Rejects CWE-59 write-through before writing to [outputPath], when:
 * - [outputPath] is itself a symlink, so the write would follow it; or
 * - an ancestor is an escaping symlink (e.g. `bin/link -> ../../outside`), so the
 *   deepest existing ancestor's real path lands outside [realTargetBase].
 *
 * Also used for symlink entries: the link may point outside, but it must be created
 * inside [realTargetBase].
 */
private fun requireRealPathInsideTarget(
    targetBase: Path,
    realTargetBase: Path,
    outputPath: Path,
    verifiedAncestors: MutableSet<Path>
) {
    // A write to an existing symlink would follow it.
    if (Files.isSymbolicLink(outputPath)) {
        throw TarExtractionSecurityException(
            "Entry '${targetBase.relativize(outputPath)}' would overwrite an existing symlink"
        )
    }

    // Check the real path of the deepest existing ancestor.
    var ancestor: Path? = outputPath.parent
    while (ancestor != null && !Files.exists(ancestor, LinkOption.NOFOLLOW_LINKS)) {
        ancestor = ancestor.parent
    }
    if (ancestor == null) return
    if (ancestor in verifiedAncestors) return

    val realAncestor = ancestor.toRealPath()
    if (!realAncestor.startsWith(realTargetBase)) {
        throw TarExtractionSecurityException(
            "Entry '${targetBase.relativize(outputPath)}' would be written outside target directory via symlink"
        )
    }
    verifiedAncestors.add(ancestor)
}

// TAR hardlink targets are archive-root-relative, not link-location-relative.
private fun validateHardlinkTarget(targetBase: Path, linkName: String): Path {
    val targetPath = targetBase.resolve(linkName).normalize()
    requireInsideTarget(targetBase, targetPath, "Hardlink target '$linkName'")
    return targetPath
}

private fun requireInsideTarget(targetBase: Path, candidate: Path, description: String) {
    if (!candidate.startsWith(targetBase)) {
        throw TarExtractionSecurityException("$description escapes target directory")
    }
}

private fun getPosixFilePermissions(mode: Int): Set<PosixFilePermission> {
    val permissions: MutableSet<PosixFilePermission> = mutableSetOf()

    // adding owner permissions
    permissions.addPermission(mode, 0b100_000_000, PosixFilePermission.OWNER_READ)
    permissions.addPermission(mode, 0b010_000_000, PosixFilePermission.OWNER_WRITE)
    permissions.addPermission(mode, 0b001_000_000, PosixFilePermission.OWNER_EXECUTE)

    // adding group permissions
    permissions.addPermission(mode, 0b000_100_000, PosixFilePermission.GROUP_READ)
    permissions.addPermission(mode, 0b000_010_000, PosixFilePermission.GROUP_WRITE)
    permissions.addPermission(mode, 0b000_001_000, PosixFilePermission.GROUP_EXECUTE)

    // adding other permissions
    permissions.addPermission(mode, 0b000_000_100, PosixFilePermission.OTHERS_READ)
    permissions.addPermission(mode, 0b000_000_010, PosixFilePermission.OTHERS_WRITE)
    permissions.addPermission(mode, 0b000_000_001, PosixFilePermission.OTHERS_EXECUTE)

    return permissions
}

private fun MutableSet<PosixFilePermission>.addPermission(mode: Int, permissionBitMask: Int, permission: PosixFilePermission) {
    if ((mode and permissionBitMask) > 0) {
        add(permission)
    }
}

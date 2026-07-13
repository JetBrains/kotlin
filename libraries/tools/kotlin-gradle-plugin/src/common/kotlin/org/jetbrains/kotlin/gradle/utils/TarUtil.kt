/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.GZIPInputStream
import kotlin.io.path.inputStream
import kotlin.io.use

internal fun Path.unzipTarGz(destinationDirectory: Path) {
    val targetBase = destinationDirectory.normalize().toAbsolutePath()
    Files.createDirectories(targetBase)

    GZIPInputStream(BufferedInputStream(inputStream())).use { gzipInputStream ->
        val hardLinks = mutableMapOf<Path, Path>()

        TarArchiveInputStream(gzipInputStream).use { tarInputStream ->
            generateSequence {
                tarInputStream.nextEntry
            }.forEach { entry: TarArchiveEntry ->
                val outputPath = targetBase.resolve(entry.name).normalize()
                if (entry.isDirectory) {
                    Files.createDirectories(outputPath)
                } else {
                    if (entry.isSymbolicLink) {
                        Files.createDirectories(outputPath.parent)
                        Files.createSymbolicLink(outputPath, Paths.get(entry.linkName))
                    } else if (entry.isLink) {
                        hardLinks[outputPath] = targetBase.resolve(entry.linkName).normalize()
                    } else {
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
            Files.createDirectories(linkPath.parent)
            Files.createLink(linkPath, targetPath)
        }
    }
}

private val supportsPosixFilePermissions: Boolean by lazy {
    FileSystems.getDefault().supportedFileAttributeViews().contains("posix")
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

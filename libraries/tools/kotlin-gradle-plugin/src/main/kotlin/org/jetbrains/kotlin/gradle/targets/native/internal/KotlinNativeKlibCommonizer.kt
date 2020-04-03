/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.compilerRunner.KotlinNativeKlibCommonizerToolRunner
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.*
import java.time.*
import java.util.*

internal fun runCommonizerInBulk(
    project: Project,
    distributionDir: File,
    baseDestinationDir: File,
    targetGroups: List<Set<KonanTarget>>,
    kotlinVersion: String
): List<File> {
    val commandLineArguments = mutableListOf<String>()

    val successPostActions = mutableListOf<() -> Unit>()
    val failurePostActions = mutableListOf<() -> Unit>()

    val destinationDirs = targetGroups.map { targets ->
        if (targets.size == 1) {
            // no need to commonize, just use the libraries from the distribution
            distributionDir.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
        } else {
            // need stable order of targets for consistency
            val orderedTargets = targets.sortedBy { it.name }

            val discriminator = buildString {
                orderedTargets.joinTo(this, separator = "-")
                append("-")
                append(kotlinVersion.toLowerCase().base64)
            }

            val destinationDir = baseDestinationDir.resolve(discriminator)
            if (!destinationDir.isDirectory) {
                val parentDir = destinationDir.parentFile
                parentDir.mkdirs()

                val destinationTmpDir = createTempDir(
                    prefix = "tmp-" + destinationDir.name,
                    suffix = ".new",
                    directory = parentDir
                )

                commandLineArguments += "native-dist-commonize"
                commandLineArguments += "-distribution-path"
                commandLineArguments += distributionDir.toString()
                commandLineArguments += "-output-path"
                commandLineArguments += destinationTmpDir.toString()
                commandLineArguments += "-targets"
                commandLineArguments += orderedTargets.joinToString(separator = ",")

                successPostActions.add { renameDirectory(destinationTmpDir, destinationDir) }
                failurePostActions.add { renameToTempAndDelete(destinationTmpDir) }
            }

            destinationDir
        }
    }

    // first of all remove directories with unused commonized libraries plus temporary directories with commonized libraries
    // that accidentally were not cleaned up before
    cleanUp(baseDestinationDir, excludedDirectories = destinationDirs)

    try {
        callCommonizerCLI(project, commandLineArguments)
        successPostActions.forEach { it() }
    } catch (e: Exception) {
        failurePostActions.forEach { it() }
        throw e
    }

    return destinationDirs
}

private fun callCommonizerCLI(project: Project, commandLineArguments: List<String>) {
    if (commandLineArguments.isEmpty()) return

    KotlinNativeKlibCommonizerToolRunner(project).run(commandLineArguments)
}

private fun renameDirectory(source: File, destination: File) {
    val sourcePath = source.toPath()
    val destinationPath = destination.toPath()

    val suppressedExceptions = mutableListOf<IOException>()

    for (it in 0 until 3) {
        try {
            renameToTempAndDelete(destination)
            Files.move(sourcePath, destinationPath, StandardCopyOption.ATOMIC_MOVE)
            return
        } catch (e: IOException) {
            suppressedExceptions += e

            if (e is AtomicMoveNotSupportedException) {
                // new attempts have no more sense
                break
            }
        }
    }

    throw IllegalStateException("Failed to rename $source to $destination").apply { suppressedExceptions.forEach(::addSuppressed) }
}

private fun renameToTempAndDelete(directory: File) {
    if (!directory.exists()) return

    val dirToRemove = if (directory.name.startsWith("tmp-")) {
        // already temp directory, return as is
        directory
    } else {
        // first, rename the directory to some temp directory
        val tempDir = createTempFile(
            prefix = "tmp-" + directory.name,
            suffix = ".old",
            directory = directory.parentFile
        )
        tempDir.delete()

        Files.move(directory.toPath(), tempDir.toPath(), StandardCopyOption.ATOMIC_MOVE)

        tempDir
    }

    dirToRemove.deleteRecursively()
}

private fun cleanUp(baseDirectory: File, excludedDirectories: List<File>) {
    fun File.getAttributes(): BasicFileAttributes? =
        try {
            Files.readAttributes(toPath(), BasicFileAttributes::class.java)
        } catch (_: IOException) {
            null
        }

    fun FileTime.isSameOrAfter(targetInstant: Instant): Boolean {
        val fileInstant = toInstant()

        if (fileInstant.atZone(ZoneOffset.UTC).toLocalDate().year <= 1970) {
            // file time represents the epoch (or even a time point before it)
            // such instant can't be used for reliable comparison
            return false
        }

        return fileInstant >= targetInstant
    }

    val now = Instant.now()
    val oneHourAgo = now.minus(Duration.ofHours(1))
    val oneMonthAgo = now.minus(Duration.ofDays(31))

    val excludedPaths = excludedDirectories.map { it.absolutePath }.toSet()

    baseDirectory.listFiles()
        ?.forEach { file ->
            if (file.absolutePath in excludedPaths) return@forEach

            val attributes = file.getAttributes() ?: return@forEach
            if (attributes.isDirectory) {
                if (file.name.startsWith("tmp-")) {
                    // temp directories created more than 1 hour ago are stale and should be GCed
                    if (attributes.creationTime().isSameOrAfter(oneHourAgo)) return@forEach
                } else {
                    // clean up other directories which were not accesses within the last month
                    if (attributes.lastAccessTime().isSameOrAfter(oneMonthAgo)) return@forEach
                }
            } /*else {
                // clean up everything that is not a directory
            }*/

            try {
                renameToTempAndDelete(file)
            } catch (_: IOException) {
                // do nothing
            }
        }
}

private val String.base64
    get() = base64Encoder.encodeToString(toByteArray(StandardCharsets.UTF_8))

private val base64Encoder = Base64.getEncoder().withoutPadding()

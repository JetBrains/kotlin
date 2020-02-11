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
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

internal fun runCommonizerInBulk(
    project: Project,
    distributionDir: File,
    baseDestinationDir: File,
    targetGroups: List<Set<KonanTarget>>,
    kotlinVersion: String
): List<File> {
    val commandLineArguments = mutableListOf<String>()
    val postActions = mutableListOf<() -> Unit>()

    val result = targetGroups.map { targets ->
        if (targets.size == 1) {
            // no need to commonize, just use the libraries from the distribution
            distributionDir.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
        } else {
            // naive up-to-date check:
            // "X.Y.Z-SNAPSHOT" is not enough to uniquely identify the concrete version of Kotlin plugin,
            // therefore lets assume that it's always not up to date
            val definitelyNotUpToDate = kotlinVersion.endsWith("SNAPSHOT", ignoreCase = true)

            // need stable order of targets for consistency
            val orderedTargets = targets.sortedBy { it.name }

            val discriminator = buildString {
                orderedTargets.joinTo(this, separator = "-")
                append("-")
                append(kotlinVersion.toLowerCase().base64)
            }

            val destinationDir = baseDestinationDir.resolve(discriminator)
            if (definitelyNotUpToDate || !destinationDir.isDirectory) {
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

                postActions.add { renameDirectory(destinationTmpDir, destinationDir) }
            }

            destinationDir
        }
    }

    callCommonizerCLI(project, commandLineArguments)

    postActions.forEach { it() }

    return result
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
            safeDelete(destination)
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

private fun safeDelete(directory: File) {
    if (!directory.exists()) return

    // first, rename directory to some temp directory
    val tempDir = createTempFile(
        prefix = "tmp-" + directory.name,
        suffix = ".old",
        directory = directory.parentFile
    )
    tempDir.delete()

    Files.move(directory.toPath(), tempDir.toPath(), StandardCopyOption.ATOMIC_MOVE)

    // only then delete it recursively
    tempDir.deleteRecursively()
}

private val String.base64
    get() = base64Encoder.encodeToString(toByteArray(StandardCharsets.UTF_8))

private val base64Encoder = Base64.getEncoder().withoutPadding()

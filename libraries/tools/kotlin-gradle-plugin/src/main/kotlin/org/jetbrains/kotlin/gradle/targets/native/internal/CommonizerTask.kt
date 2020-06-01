/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.compilerRunner.KotlinNativeKlibCommonizerToolRunner
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.*
import java.time.*
import java.util.*
import javax.inject.Inject

internal data class CommonizerTaskParams(
    @get:InputDirectory val distributionDir: File,
    @get:InputDirectory val baseDestinationDir: File,
    @Internal val targetGroups: List<Set<KonanTarget>>,
    @get:Input val kotlinVersion: String
) {
    @Internal
    val commandLineArguments = mutableListOf<String>()

    @Internal
    val successPostActions = mutableListOf<() -> Unit>()

    @Internal
    val failurePostActions = mutableListOf<() -> Unit>()

    // It isn't best option for "up-to-date" checker (for multi project build it will be started few times)
    // but commonizer has inner check
    @get:OutputDirectories
    val destinationDirs: List<File>

    // need stable order of targets for consistency
    private val orderedTargetGroups = targetGroups.map { it.sortedBy { target -> target.name } }

    @Input
    fun getTargetNames() = orderedTargetGroups.map { it.map { target -> target.name } }

    init {
        destinationDirs = orderedTargetGroups.map { orderedTargets ->
            if (orderedTargets.size == 1) {
                // no need to commonize, just use the libraries from the distribution
                distributionDir.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            } else {
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
    }
}

internal const val COMMONIZER_TASK_NAME = "runCommonizer"

internal open class CommonizerTask @Inject constructor(
    @Nested val params: CommonizerTaskParams
) : DefaultTask() {

    @TaskAction
    fun run() {
        // first of all remove directories with unused commonized libraries plus temporary directories with commonized libraries
        // that accidentally were not cleaned up before
        cleanUp(params.baseDestinationDir, excludedDirectories = params.destinationDirs)

        try {
            callCommonizerCLI(project, params.commandLineArguments)
            params.successPostActions.forEach { it() }
        } catch (e: Exception) {
            params.failurePostActions.forEach { it() }
            throw e
        }
    }
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

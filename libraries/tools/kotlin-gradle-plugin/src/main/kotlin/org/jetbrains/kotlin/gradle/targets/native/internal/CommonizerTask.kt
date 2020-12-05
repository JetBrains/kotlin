/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.compilerRunner.KotlinNativeKlibCommonizerToolRunner
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.*
import java.time.*
import java.util.*
import javax.inject.Inject

/**
 * Note: Using [resultingLibsDirs] isn't the safest option for up-to-date checker, as in multi-project build
 * this may cause re-running the commonizer for the same groups several times. Hopefully, the commonizer has
 * inner up-to-date check that prevents doing extra work.
 */
internal data class CommonizerSubtaskParams(
    // The ordered list of unique targets.
    @get:Input val orderedTargetNames: List<String>,

    // Only for up-to-date checker. The directories with the resulting libs
    // (common first, then platforms in the same order as in 'orderedTargetNames').
    @get:OutputDirectories val resultingLibsDirs: List<File>,

    // Only for up-to-date checker. The file exists if and only if a commonizer subtask was successfully accomplished.
    @get:OutputFile val successMarker: File,

    @get:Internal val destinationDir: File
)

internal data class CommonizerTaskParams(
    @get:Input val kotlinVersion: String,

    // Only for up-to-date checker. The directory with the original common libs.
    @get:InputDirectory val originalCommonLibsDir: File,

    // Only for up-to-date checker. The directory with the original platform libs.
    @get:InputDirectory val originalPlatformLibsDir: File,

    @get:Internal val baseDestinationDir: File,

    @get:Nested val subtasks: List<CommonizerSubtaskParams>
) {
    @get:Internal
    lateinit var commandLineArguments: List<String>

    @get:Internal
    lateinit var successPostActions: List<() -> Unit>

    @get:Internal
    lateinit var failurePostActions: List<() -> Unit>

    companion object {
        private const val SUCCESS_MARKER = ".commonized"
        private const val SUCCESS_MARKER_CONTENT = "1"

        fun build(
            kotlinVersion: String,
            targetGroups: List<Set<KonanTarget>>,
            distributionDir: File,
            baseDestinationDir: File
        ): CommonizerTaskParams {
            val distributionLibsDir = distributionDir.resolve(KONAN_DISTRIBUTION_KLIB_DIR)

            val commandLineArguments = mutableListOf<String>()
            val successPostActions = mutableListOf<() -> Unit>()
            val failurePostActions = mutableListOf<() -> Unit>()

            val subtasks = targetGroups.map { targets ->
                val orderedTargetNames = targets.map { it.name }.sorted()
                if (orderedTargetNames.size == 1) {
                    // no need to commonize, just use the libraries from the distribution
                    val successMarker = successMarker(distributionLibsDir).also(::writeSuccess)
                    buildSubtask(
                        destinationDir = distributionLibsDir,
                        orderedTargetNames = orderedTargetNames,
                        successMarker = successMarker
                    )
                } else {
                    val discriminator = buildString {
                        orderedTargetNames.joinTo(this, separator = "-")
                        append("-")
                        append(kotlinVersion.toLowerCase().base64)
                    }

                    val destinationDir = baseDestinationDir.resolve(discriminator)
                    val successMarker = successMarker(destinationDir)

                    if (!isSuccess(successMarker)) {
                        successMarker.delete()

                        val parentDir = destinationDir.parentFile
                        parentDir.mkdirs()

                        val destinationTmpDir = Files.createTempDirectory(
                            /* dir = */ parentDir.toPath(),
                            /* prefix = */ "tmp-new-" + destinationDir.name
                        ).toFile()

                        commandLineArguments += "native-dist-commonize"
                        commandLineArguments += "-distribution-path"
                        commandLineArguments += distributionDir.toString()
                        commandLineArguments += "-output-path"
                        commandLineArguments += destinationTmpDir.toString()
                        commandLineArguments += "-targets"
                        commandLineArguments += orderedTargetNames.joinToString(separator = ",")

                        successPostActions.add {
                            renameDirectory(destinationTmpDir, destinationDir)
                            writeSuccess(successMarker)
                        }

                        failurePostActions.add {
                            renameToTempAndDelete(destinationTmpDir)
                        }
                    }

                    buildSubtask(
                        destinationDir = destinationDir,
                        orderedTargetNames = orderedTargetNames,
                        successMarker = successMarker
                    )
                }
            }

            return CommonizerTaskParams(
                kotlinVersion = kotlinVersion,
                originalCommonLibsDir = commonLibsDir(distributionLibsDir),
                originalPlatformLibsDir = platformLibsDir(distributionLibsDir),
                baseDestinationDir = baseDestinationDir,
                subtasks = subtasks
            ).also {
                it.commandLineArguments = commandLineArguments
                it.successPostActions = successPostActions
                it.failurePostActions = failurePostActions
            }
        }

        private fun commonLibsDir(baseDir: File): File = baseDir.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
        private fun platformLibsDir(baseDir: File): File = baseDir.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)

        private fun platformLibsDirs(baseDir: File, orderedTargetNames: List<String>): List<File> {
            val platformLibsDir = platformLibsDir(baseDir)
            return orderedTargetNames.map(platformLibsDir::resolve)
        }

        private fun resultingLibsDirs(baseDir: File, orderedTargetNames: List<String>): List<File> {
            return mutableListOf<File>().apply {
                this += commonLibsDir(baseDir)
                this += platformLibsDirs(baseDir, orderedTargetNames)
            }
        }

        private fun buildSubtask(
            destinationDir: File,
            orderedTargetNames: List<String>,
            successMarker: File
        ) = CommonizerSubtaskParams(
            orderedTargetNames = orderedTargetNames,
            resultingLibsDirs = resultingLibsDirs(destinationDir, orderedTargetNames),
            successMarker = successMarker,
            destinationDir = destinationDir
        )

        private fun successMarker(destinationDir: File) = destinationDir.resolve(SUCCESS_MARKER)
        private fun isSuccess(successMarker: File) = successMarker.isFile && successMarker.readText() == SUCCESS_MARKER_CONTENT

        private fun writeSuccess(successMarker: File) {
            if (successMarker.exists()) {
                when {
                    successMarker.isDirectory -> renameToTempAndDelete(successMarker)
                    isSuccess(successMarker) -> return
                    else -> successMarker.delete()
                }
            }

            successMarker.writeText(SUCCESS_MARKER_CONTENT)
        }
    }
}

internal const val COMMONIZER_TASK_NAME = "runCommonizer"

internal open class CommonizerTask @Inject constructor(
    @get:Nested val params: CommonizerTaskParams
) : DefaultTask() {

    @TaskAction
    fun run() {
        // first of all remove directories with unused commonized libraries plus temporary directories with commonized libraries
        // that accidentally were not cleaned up before
        cleanUp(
            baseDirectory = params.baseDestinationDir,
            excludedDirectories = params.subtasks.map { it.destinationDir }
        )

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
        val tempDir = Files.createTempFile(
            /* dir = */ directory.parentFile.toPath(),
            /* prefix = */ "tmp-old-" + directory.name,
            /* suffix = */ null
        )
        Files.delete(tempDir)

        Files.move(directory.toPath(), tempDir, StandardCopyOption.ATOMIC_MOVE)

        tempDir.toFile()
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

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCommonizerToolRunner
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.targets.native.internal.SuccessMarker.Companion.getSuccessMarker
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR
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

internal const val COMMONIZER_TASK_NAME = "runCommonizer"

internal typealias KonanTargetGroup = Set<KonanTarget>

internal open class CommonizerTask : DefaultTask() {

    private val konanHome = project.file(project.konanHome)

    @get:Input
    var targetGroups: Set<KonanTargetGroup> = emptySet()

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    @Suppress("unused") // Only for up-to-date checker. The directory with the original common libs.
    val originalCommonLibrariesDirectory = konanHome
        .resolve(KONAN_DISTRIBUTION_KLIB_DIR)
        .resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    @Suppress("unused") // Only for up-to-date checker. The directory with the original platform libs.
    val originalPlatformLibrariesDirectory = konanHome
        .resolve(KONAN_DISTRIBUTION_KLIB_DIR)
        .resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)

    @get:OutputDirectories
    val commonizerTargetOutputDirectories
        get() = targetGroups.map { targets -> project.nativeDistributionCommonizerOutputDirectory(targets) }

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFiles
    @Suppress("unused") // Only for up-to-date checker.
    val successMarkers
        get() = targetGroups.map { targets -> project.getSuccessMarker(targets).file }

    /*
    Ensures that only one CommonizerTask can run at a time.
    This is necessary because of the sucess-marker mechansim of this task.
    This is a phantom file: No one has the intention to actually create this output file.
    However, telling Gradle that all those tasks rely on the same output file will enforce
    non-parallel execution.
     */
    @get:OutputFile
    @Suppress("unused")
    val taskMutex: File = project.rootProject.file(".commonizer-phantom-output")

    @TaskAction
    fun run() {
        // first of all remove directories with unused commonized libraries plus temporary directories with commonized libraries
        // that accidentally were not cleaned up before
        cleanUp(
            baseDirectory = konanHome.resolve(KONAN_DISTRIBUTION_KLIB_DIR).resolve(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR),
            excludedDirectories = commonizerTargetOutputDirectories
        )

        val executionEnvironment = createExecutionEnvironment()

        try {
            callCommonizerCLI(project, executionEnvironment.commandLineArguments)
            executionEnvironment.stagedDirectories.forEach { stagedDirectory -> stagedDirectory.onSuccess() }
            executionEnvironment.successMarkers.forEach { successMarker -> successMarker.writeSuccess() }
        } catch (e: Throwable) {
            executionEnvironment.stagedDirectories.forEach { stagedDirectory -> stagedDirectory.onFailure() }
            executionEnvironment.successMarkers.forEach { successMarker -> successMarker.delete() }
            throw e
        }
    }

    private fun createExecutionEnvironment(): CommonizerExecutionEnvironment {
        val stagedDirectories = mutableListOf<TemporaryStagedDirectory>()
        val successMarkers = mutableListOf<SuccessMarker>()
        val arguments = mutableListOf<String>()

        targetGroups.forEach { targets ->
            // no need to commonize, just use the libraries from the distribution
            if (targets.size <= 1) return@forEach
            val orderedTargetNames = targets.map { it.name }.sorted()
            val successMarker = project.getSuccessMarker(targets)
            if (successMarker.isSuccess) return@forEach

            val stagedDirectory = TemporaryStagedDirectory(
                temporaryDirectoryFile = project.createTempNativeDistributionCommonizerOutputDirectory(targets),
                targetDirectoryFile = project.nativeDistributionCommonizerOutputDirectory(targets)
            )

            stagedDirectories += stagedDirectory
            successMarkers += successMarker
            arguments += "native-dist-commonize"
            arguments += "-distribution-path"
            arguments += konanHome.absolutePath
            arguments += "-output-path"
            arguments += stagedDirectory.temporaryDirectoryFile.absolutePath
            arguments += "-targets"
            arguments += orderedTargetNames.joinToString(separator = ",")
        }

        return CommonizerExecutionEnvironment(
            commandLineArguments = arguments,
            successMarkers = successMarkers,
            stagedDirectories = stagedDirectories
        )
    }
}

private class CommonizerExecutionEnvironment(
    val commandLineArguments: List<String>,
    val successMarkers: List<SuccessMarker>,
    val stagedDirectories: List<TemporaryStagedDirectory>
)

private class SuccessMarker private constructor(val file: File) {
    companion object {
        private const val SUCCESS_MARKER = ".commonized"
        private const val SUCCESS_MARKER_CONTENT = "1"

        fun Project.getSuccessMarker(targets: KonanTargetGroup): SuccessMarker {
            return SuccessMarker(nativeDistributionCommonizerOutputDirectory(targets).resolve(SUCCESS_MARKER))
        }
    }

    val isSuccess get() = file.isFile && file.readText() == SUCCESS_MARKER_CONTENT

    fun delete(): Boolean = file.delete()

    fun writeSuccess() {
        if (isSuccess) return
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        if (file.isDirectory) {
            renameToTempAndDelete(file)
        }
        file.writeText(SUCCESS_MARKER_CONTENT)
    }
}

private class TemporaryStagedDirectory(val temporaryDirectoryFile: File, private val targetDirectoryFile: File) {
    fun onFailure() = renameToTempAndDelete(temporaryDirectoryFile)
    fun onSuccess() = renameDirectory(temporaryDirectoryFile, targetDirectoryFile)
}

internal fun Project.nativeDistributionCommonizerOutputDirectory(targets: KonanTargetGroup): File {
    val kotlinVersion = checkNotNull(project.getKotlinPluginVersion()) { "Failed infering Kotlin Plugin version" }
    val orderedTargetNames = targets.map { it.name }.sorted()
    val discriminator = buildString {
        orderedTargetNames.joinTo(this, separator = "-")
        append("-")
        append(kotlinVersion.toLowerCase().base64)
    }
    return project.file(konanHome)
        .resolve(KONAN_DISTRIBUTION_KLIB_DIR)
        .resolve(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR)
        .resolve(discriminator)
}

internal fun Project.createTempNativeDistributionCommonizerOutputDirectory(targets: KonanTargetGroup): File {
    val outputDirectory = nativeDistributionCommonizerOutputDirectory(targets)
    outputDirectory.parentFile.mkdirs()
    return Files.createTempDirectory(
        /* dir = */ outputDirectory.parentFile.toPath(),
        /* prefix = */ "tmp-new-${outputDirectory.name}"
    ).toFile()
}

fun callCommonizerCLI(project: Project, commandLineArguments: List<String>) {
    if (commandLineArguments.isEmpty()) return

    KotlinNativeCommonizerToolRunner(project).run(commandLineArguments)
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

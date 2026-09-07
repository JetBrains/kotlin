/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_ALLOW_INCOMPLETE_KOTLIN_ARCHIVE_PUBLICATION
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinProjectSharedDataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.CrossCompilationData
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.io.OutputStream
import java.util.zip.Deflater
import javax.inject.Inject


internal class KotlinArchiveEntry(
    @get:Input
    val pathPrefix: Provider<String>,
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val files: FileCollection,
)

internal class KotlinArchiveTargetCrossCompilationCheckData(
    @get:Input
    val targetName: String,
    @get:Internal
    val crossCompilationData: KotlinProjectSharedDataProvider<CrossCompilationData>,
) {
    /**
     * This is here purely for dependency management purposes.
     * Task doesn't need it directly, but it's used in implementation of called functions
     * from [crossCompilationData].
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    val crossCompilationMetadata: FileCollection = crossCompilationData.files
}

internal fun KotlinArchiveTargetCrossCompilationCheckData.isSupported(): Boolean {
    return crossCompilationData.dataForAllDependencies.all { it.crossCompilationSupported }
}

@DisableCachingByDefault(because = "Assembling a Kotlin Archive is not worth caching, as it's only built for publishing, which is a rare operation")
internal abstract class AssembleKotlinArchiveTask @Inject constructor(
    private val fileOperations: FileOperations,
) : DefaultTask(), UsesKotlinToolingDiagnostics {
    @get:Nested
    abstract val archiveContents: ListProperty<KotlinArchiveEntry>

    private fun kotlinArchiveEntryOf(path: String, files: FileCollection) = KotlinArchiveEntry(project.provider { path }, files)

    fun addPlatformKlib(path: String, files: FileCollection) {
        archiveContents.add(kotlinArchiveEntryOf("${KarLayout.PLATFORM_KLIBS_DIRECTORY_NAME}/$path", files))
    }

    fun addCInterop(pathProvider: Provider<String>, files: FileCollection) {
        archiveContents.add(KotlinArchiveEntry(pathProvider.map { path -> "${KarLayout.CINTEROP_KLIBS_DIRECTORY_NAME}/$path" }, files))
    }

    fun addMetadataKlib(path: String, files: FileCollection) {
        archiveContents.add(kotlinArchiveEntryOf("${KarLayout.METADATA_DIRECTORY_NAME}/$path", files))
    }

    fun addResources(path: String, files: FileCollection) {
        archiveContents.add(kotlinArchiveEntryOf("${KarLayout.RESOURCES_DIRECTORY_NAME}/$path", files))
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val projectStructureMetadataFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val targetsNotPublishableOnCurrentHost: SetProperty<String>

    /**
     * This check can't be done on configuration phase, as it requires to resolve the dependencies,
     * and maybe even run the tasks to generate files from them.
     *
     * Also, we can't wrap the sharedData to conditionally empty provider and put it into [targetsNotPublishableOnCurrentHost],
     * but need to store it directly.
     * Otherwise, when configuration cache is enabled, it would be queried too early (when cache is stored).
     *
     * So we just store relevant data to perform check in task runtime.
     */
    @get:Nested
    abstract val targetsCrossCompilationChecks: ListProperty<KotlinArchiveTargetCrossCompilationCheckData>

    fun checkTargetHasNoMissingDependenciesBecauseOfCrossCompilationDisabled(
        targetName: String,
        crossCompilationData: KotlinProjectSharedDataProvider<CrossCompilationData>
    ) {
        targetsCrossCompilationChecks.add(
            KotlinArchiveTargetCrossCompilationCheckData(
                targetName = targetName,
                crossCompilationData = crossCompilationData
            )
        )
    }

    @get:Input
    abstract val incompleteArchiveAllowed: Property<Boolean>

    private fun checkAllTargetsArePublishable() {
        val targetsNonPublishableBecauseOfMissingCrosscompiledDependencies =
            targetsCrossCompilationChecks.get()
                .filter { !it.isSupported() }
                .map { it.targetName }
        val allNonPublishableTargets =
            targetsNotPublishableOnCurrentHost.get() +
            targetsNonPublishableBecauseOfMissingCrosscompiledDependencies

        if (allNonPublishableTargets.isEmpty()) return

        val diagnostic = KotlinToolingDiagnostics.IncompleteKotlinArchivePublication(
            allNonPublishableTargets,
            HostManager.platformName(),
        )

        if (incompleteArchiveAllowed.get()) {
            logger.info(
                "Kotlin Archive is built without $allNonPublishableTargets, " +
                        "as it is allowed by the '$KOTLIN_ALLOW_INCOMPLETE_KOTLIN_ARCHIVE_PUBLICATION' property"
            )
            return
        }

        reportDiagnostic(diagnostic)
    }

    @TaskAction
    fun execute() {
        checkAllTargetsArePublishable()

        val targetDir = outputDirectory.get().asFile

        val rootDirectories = listOf(
            KarLayout.PLATFORM_KLIBS_DIRECTORY_NAME,
            KarLayout.CINTEROP_KLIBS_DIRECTORY_NAME,
            KarLayout.METADATA_DIRECTORY_NAME,
            KarLayout.RESOURCES_DIRECTORY_NAME,
        )

        fileOperations.sync { spec ->
            spec.into(targetDir)
            for (input in archiveContents.get()) {
                spec.from(input.files) { inputSpec ->
                    inputSpec.into(input.pathPrefix)
                }
            }
            spec.from(projectStructureMetadataFile) { psmSpec ->
                psmSpec.into(KarLayout.METADATA_DIRECTORY_NAME)
                psmSpec.rename { KarLayout.PSM_FILE_NAME }
            }
        }

        // Create directories manually in case some of have no content (in that case sync doesn't create them)
        for (directory in rootDirectories) {
            targetDir.resolve(directory).mkdirs()
        }

        targetDir.resolve(KarLayout.MANIFEST_FILE_PATH).outputStream().use {
            it.putManifestContent()
        }
    }

    // For now, nothing real is stored in manifest.
    // It's just used for future extensibility
    private fun OutputStream.putManifestContent() {
        write(
            """
                {
                    "version": "1.0"
                }
            """.trimIndent().toByteArray()
        )
    }
}

@DisableCachingByDefault(because = "Packing a Kotlin Archive is not worth caching, as it's only built for publishing, which is a rare operation")
internal abstract class PackKotlinArchiveTask @Inject constructor(
    private val fileOperations: FileOperations,
) : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val assembledKarDirectory: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * We want to put files with the same name nearby each other in archive.
     * Files with the same name would correspond to platform-specific version of the same thing.
     * In most cases, they should be very similar to each other, so they would be compressed much better,
     * if located nearby.
     *
     * On kotlinx-coroutines-core, it improves compression by extra 10%.
     * This difference should be larger as total klib size growth.
     */
    private fun prepareArchiveFiles(): List<ArchiveFile> {
        val assembledKarDirectory = assembledKarDirectory.get().asFile
        return buildList {
            fileOperations.fileTree(assembledKarDirectory).visit(object : ReproducibleFileVisitor {
                override fun isReproducibleFileOrder(): Boolean = true

                override fun visitDir(details: FileVisitDetails) = addEntry(details, isDirectory = true)
                override fun visitFile(details: FileVisitDetails) = addEntry(details, isDirectory = false)

                private fun addEntry(details: FileVisitDetails, isDirectory: Boolean) {
                    add(
                        ArchiveFile(
                            path = details.path,
                            source = assembledKarDirectory.resolve(details.path),
                            isDirectory = isDirectory,
                            name = details.name,
                        )
                    )
                }
            })
            sortBy { it.name }
        }
    }

    @TaskAction
    fun execute() {
        outputFile.get().asFile.apply { parentFile.mkdirs() }.outputStream().buffered().use { output ->
            XZCompressorOutputStream(output).use { compressedOutput ->
                ZipArchiveOutputStream(compressedOutput).use { zipOutput ->
                    zipOutput.setLevel(Deflater.NO_COMPRESSION)

                    for ((path, source, isDirectory) in prepareArchiveFiles()) {
                        if (isDirectory) {
                            zipOutput.directoryEntry(path)
                        } else {
                            zipOutput.entry(path) {
                                source.inputStream().use { input ->
                                    input.copyTo(this)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ZipArchiveOutputStream.directoryEntry(entryPath: String) {
        entry("$entryPath/") {}
    }

    private fun ZipArchiveOutputStream.entry(path: String, content: OutputStream.() -> Unit) {
        val entry = ZipArchiveEntry(path).apply {
            this.time = 0
        }

        putArchiveEntry(entry)
        content()
        closeArchiveEntry()
    }
}

private data class ArchiveFile(
    val path: String,
    val source: File,
    val isDirectory: Boolean,
    val name: String,
)

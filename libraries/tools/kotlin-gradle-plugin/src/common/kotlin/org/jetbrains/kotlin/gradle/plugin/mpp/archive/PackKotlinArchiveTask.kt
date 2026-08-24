/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.ReproducibleFileVisitor
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.OutputStream
import java.util.zip.Deflater
import javax.inject.Inject


internal class KotlinArchiveEntry(
    @get:Input
    val pathPrefix: Provider<String>,
    @get:InputFiles
    val files: FileCollection,
)

@DisableCachingByDefault(because = "Assembling a Kotlin Archive is not worth caching, as it's only built for publishing, which is a rare operation")
internal abstract class AssembleKotlinArchiveTask @Inject constructor(
    private val fileOperations: FileOperations,
) : DefaultTask() {
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
    abstract val projectStructureMetadataFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun execute() {
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
                psmSpec.rename { KarLayout.PSM_FILE_PATH.substringAfterLast('/') }
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

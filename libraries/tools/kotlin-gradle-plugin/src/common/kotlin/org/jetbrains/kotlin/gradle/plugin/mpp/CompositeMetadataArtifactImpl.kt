/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.utils.checksumString
import org.jetbrains.kotlin.gradle.utils.copyPartially
import org.jetbrains.kotlin.gradle.utils.ensureValidZipDirectoryPath
import org.jetbrains.kotlin.gradle.utils.listDescendants
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

internal class CompositeMetadataArtifactImpl(
    override val moduleDependencyIdentifier: ModuleDependencyIdentifier,
    override val moduleDependencyVersion: String,
    private val kotlinProjectStructureMetadata: KotlinProjectStructureMetadata,
    private val primaryArtifactFile: File,
    private val hostSpecificArtifactFilesBySourceSetName: Map<String, File>
) : CompositeMetadataArtifact {

    override fun exists(): Boolean {
        return primaryArtifactFile.exists() && hostSpecificArtifactFilesBySourceSetName.values.all { it.exists() }
    }

    override fun open(): CompositeMetadataArtifactContent {
        return CompositeMetadataArtifactContentImpl()
    }

    inner class CompositeMetadataArtifactContentImpl : CompositeMetadataArtifactContent {

        override val containingArtifact: CompositeMetadataArtifact
            get() = this@CompositeMetadataArtifactImpl

        /* Creating SourceSet instances eagerly, as they will only lazily access files */
        private val sourceSetsImpl = kotlinProjectStructureMetadata.sourceSetNames.associateWith { sourceSetName ->
            SourceSetContentImpl(this, sourceSetName, ArtifactFile(hostSpecificArtifactFilesBySourceSetName[sourceSetName] ?: primaryArtifactFile))
        }

        override val sourceSets: List<CompositeMetadataArtifactContent.SourceSetContent> =
            sourceSetsImpl.values.toList()

        override fun getSourceSet(name: String): CompositeMetadataArtifactContent.SourceSetContent {
            return findSourceSet(name)
                ?: throw IllegalArgumentException("No SourceSet with name $name found. Known SourceSets: ${sourceSetsImpl.keys}")
        }

        override fun findSourceSet(name: String): CompositeMetadataArtifactContent.SourceSetContent? =
            sourceSetsImpl[name]


        override fun close() {
            sourceSetsImpl.values.forEach { it.close() }
        }
    }

    private inner class SourceSetContentImpl(
        override val containingArtifactContent: CompositeMetadataArtifactContent,
        override val sourceSetName: String,
        private val artifactFile: ArtifactFile
    ) : CompositeMetadataArtifactContent.SourceSetContent, Closeable {

        override val metadataBinary: CompositeMetadataArtifactContent.MetadataBinary? by lazy {
            /*
            There are published multiplatform libraries that indeed suppress, disable certain compilations.
            In this scenario, the sourceSetName might still be mentioned in the artifact, but there will be no
            metadata-library packaged into the composite artifact.

            In this case, return null
             */
            if (artifactFile.containsDirectory(sourceSetName)) MetadataBinaryImpl(this, artifactFile) else null
        }

        override val cinteropMetadataBinaries: List<CompositeMetadataArtifactContent.CInteropMetadataBinary> by lazy {
            val cinteropMetadataDirectory = kotlinProjectStructureMetadata.sourceSetCInteropMetadataDirectory[sourceSetName]
                ?: return@lazy emptyList()

            val cinteropMetadataDirectoryPath = ensureValidZipDirectoryPath(cinteropMetadataDirectory)
            val cinteropEntries = artifactFile.zip.listDescendants(cinteropMetadataDirectoryPath)

            val cinteropLibraryNames = cinteropEntries.map { entry ->
                entry.name.removePrefix(cinteropMetadataDirectoryPath).split("/", limit = 2).first()
            }.toSet()

            cinteropLibraryNames.map { cinteropLibraryName ->
                CInteropMetadataBinaryImpl(this, cinteropLibraryName, artifactFile)
            }
        }

        override fun close() {
            artifactFile.close()
        }
    }

    private inner class MetadataBinaryImpl(
        override val containingSourceSetContent: CompositeMetadataArtifactContent.SourceSetContent,
        private val artifactFile: ArtifactFile
    ) : CompositeMetadataArtifactContent.MetadataBinary {

        override val archiveExtension: String
            get() = kotlinProjectStructureMetadata.sourceSetBinaryLayout[containingSourceSetContent.sourceSetName]?.archiveExtension
                ?: SourceSetMetadataLayout.METADATA.archiveExtension

        override val checksum: String
            get() = artifactFile.checksum

        /**
         * Example:
         * org.jetbrains.sample-sampleLibrary-1.0.0-SNAPSHOT-appleAndLinuxMain-Vk5pxQ.klib
         */
        override val relativeFile: File = File(buildString {
            append(containingSourceSetContent.containingArtifactContent.containingArtifact.moduleDependencyIdentifier)
            append("-")
            append(containingSourceSetContent.containingArtifactContent.containingArtifact.moduleDependencyVersion)
            append("-")
            append(containingSourceSetContent.sourceSetName)
            append("-")
            append(this@MetadataBinaryImpl.checksum)
            append(".")
            append(archiveExtension)
        })

        override fun copyTo(file: File): Boolean {
            require(file.extension == archiveExtension) {
                "Expected file.extension == '$archiveExtension'. Found ${file.extension}"
            }

            val libraryPath = "${containingSourceSetContent.sourceSetName}/"
            if (!artifactFile.containsDirectory(libraryPath)) return false
            file.parentFile.mkdirs()
            artifactFile.zip.copyPartially(file, libraryPath)

            return true
        }
    }

    private inner class CInteropMetadataBinaryImpl(
        override val containingSourceSetContent: CompositeMetadataArtifactContent.SourceSetContent,
        override val cinteropLibraryName: String,
        private val artifactFile: ArtifactFile,
    ) : CompositeMetadataArtifactContent.CInteropMetadataBinary {

        override val archiveExtension: String
            get() = SourceSetMetadataLayout.KLIB.archiveExtension

        override val checksum: String
            get() = artifactFile.checksum

        /**
         * Example:
         * org.jetbrains.sample-sampleLibrary-1.0.0-SNAPSHOT-appleAndLinuxMain-cinterop/
         *     org.jetbrains.sample_sampleLibrary-cinterop-simple-Vk5pxQ.klib
         */
        override val relativeFile: File = File(buildString {
            append(containingSourceSetContent.containingArtifactContent.containingArtifact.moduleDependencyIdentifier)
            append("-")
            append(containingSourceSetContent.containingArtifactContent.containingArtifact.moduleDependencyVersion)
            append("-")
            append(containingSourceSetContent.sourceSetName)
            append("-cinterop")
        }).resolve("$cinteropLibraryName-${this.checksum}.${archiveExtension}")

        override fun copyTo(file: File): Boolean {
            require(file.extension == archiveExtension) {
                "Expected 'file.extension == '${SourceSetMetadataLayout.KLIB.archiveExtension}'. Found ${file.extension}"
            }

            val sourceSetName = containingSourceSetContent.sourceSetName
            val cinteropMetadataDirectory = kotlinProjectStructureMetadata.sourceSetCInteropMetadataDirectory[sourceSetName]
                ?: error("Missing CInteropMetadataDirectory for SourceSet $sourceSetName")
            val cinteropMetadataDirectoryPath = ensureValidZipDirectoryPath(cinteropMetadataDirectory)

            val libraryPath = "$cinteropMetadataDirectoryPath$cinteropLibraryName/"
            if (!artifactFile.containsDirectory(libraryPath)) return false
            file.parentFile.mkdirs()
            artifactFile.zip.copyPartially(file, "$cinteropMetadataDirectoryPath$cinteropLibraryName/")

            return true
        }
    }

    /**
     * Interface to the underlying [zip][file] that only opens the file lazily and keeps references to
     * all [entries] and infers all potential directory paths (see [directoryPaths] and [containsDirectory])
     */
    private class ArtifactFile(private val file: File) : Closeable {

        private var isClosed = false

        private val lazyZip = lazy {
            ensureNotClosed()
            ZipFile(file)
        }

        val zip: ZipFile get() = lazyZip.value

        val entries: List<ZipEntry> by lazy {
            zip.entries().toList()
        }

        val checksum: String by lazy(LazyThreadSafetyMode.NONE) {
            val crc32 = CRC32()
            entries.forEach { entry -> crc32.update(entry.crc.toInt()) }
            checksumString(crc32.value.toInt())
        }

        /**
         * All potential directory paths, including inferred directory paths when the [zip] file does
         * not include directory entries.
         * @see collectAllDirectoryPaths
         */
        val directoryPaths: Set<String> by lazy { collectAllDirectoryPaths(entries) }

        /**
         * Check if the underlying [zip] file contains this directory.
         * Note: This check also works for zip files that did not include directory entries.
         * This will return true, if any other zip-entry is placed inside this directory [path]
         */
        fun containsDirectory(path: String): Boolean {
            val validPath = ensureValidZipDirectoryPath(path)
            if (zip.getEntry(validPath) != null) return true
            return validPath in directoryPaths
        }

        private fun ensureNotClosed() {
            if (isClosed) throw IOException("LazyZipFile is already closed!")
        }

        override fun close() {
            isClosed = true
            if (lazyZip.isInitialized()) {
                lazyZip.value.close()
            }
        }
    }
}

/**
 * Zip files are not **forced** to include entries for directories.
 * In order to do preliminary checks, if some directory is present in Zip Files it is
 * often useful to infer the directories included in any Zip File by looking into file entries
 * and inferring their directories.
 */
private fun collectAllDirectoryPaths(entries: List<ZipEntry>): Set<String> {
    /*
    The 'root' directory is represented as empty String in ZipFile
     */
    val set = hashSetOf("")

    entries.forEach { entry ->
        if (entry.isDirectory) {
            set.add(entry.name)
            return@forEach
        }

        /* Collect all 'intermediate' directories found by looking at the files path */
        val pathParts = entry.name.split("/")
        pathParts.runningReduce { currentPath, nextPart ->
            set.add("$currentPath/")
            "$currentPath/$nextPart"
        }
    }
    return set
}


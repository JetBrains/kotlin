/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.utils.copyPartially
import org.jetbrains.kotlin.gradle.utils.ensureValidZipDirectoryPath
import org.jetbrains.kotlin.gradle.utils.listDescendants
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.Base64.Encoder
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

    override fun open(): CompositeMetadataArtifact.ArtifactContent {
        return HandlerImpl()
    }

    inner class HandlerImpl : CompositeMetadataArtifact.ArtifactContent {

        override val moduleDependencyIdentifier: ModuleDependencyIdentifier
            get() = this@CompositeMetadataArtifactImpl.moduleDependencyIdentifier

        override val moduleDependencyVersion: String
            get() = this@CompositeMetadataArtifactImpl.moduleDependencyVersion

        /* Creating SourceSet instances eagerly, as they will only lazily access files */
        private val sourceSetsImpl = kotlinProjectStructureMetadata.sourceSetNames.associateWith { sourceSetName ->
            SourceSetImpl(this, sourceSetName, ArtifactFile(hostSpecificArtifactFilesBySourceSetName[sourceSetName] ?: primaryArtifactFile))
        }

        override val sourceSets: List<CompositeMetadataArtifact.SourceSet> =
            sourceSetsImpl.values.toList()

        override fun getSourceSet(name: String): CompositeMetadataArtifact.SourceSet {
            return findSourceSet(name)
                ?: throw IllegalArgumentException("No SourceSet with name $name found. Known SourceSets: ${sourceSetsImpl.keys}")
        }

        override fun findSourceSet(name: String): CompositeMetadataArtifact.SourceSet? =
            sourceSetsImpl[name]


        override fun close() {
            sourceSetsImpl.values.forEach { it.close() }
        }
    }

    private inner class SourceSetImpl(
        override val artifactContent: CompositeMetadataArtifact.ArtifactContent,
        override val sourceSetName: String,
        private val artifactFile: ArtifactFile
    ) : CompositeMetadataArtifact.SourceSet, Closeable {

        override val metadataLibrary: CompositeMetadataArtifact.MetadataLibrary? by lazy {
            /*
            There are published multiplatform libraries that indeed suppress, disable certain compilations.
            In this scenario, the sourceSetName might still be mentioned in the artifact, but there will be no
            metadata-library packaged into the composite artifact.

            In this case, return null
             */
            if (artifactFile.containsDirectory(sourceSetName)) MetadataLibraryImpl(artifactContent, this, artifactFile) else null
        }

        override val cinteropMetadataLibraries: List<CompositeMetadataArtifact.CInteropMetadataLibrary> by lazy {
            val cinteropMetadataDirectory = kotlinProjectStructureMetadata.sourceSetCInteropMetadataDirectory[sourceSetName]
                ?: return@lazy emptyList()

            val cinteropMetadataDirectoryPath = ensureValidZipDirectoryPath(cinteropMetadataDirectory)
            val cinteropEntries = artifactFile.zip.listDescendants(cinteropMetadataDirectoryPath)

            val cinteropLibraryNames = cinteropEntries.map { entry ->
                entry.name.removePrefix(cinteropMetadataDirectoryPath).split("/", limit = 2).first()
            }.toSet()

            cinteropLibraryNames.map { cinteropLibraryName ->
                CInteropMetadataLibraryImpl(artifactContent, this, cinteropLibraryName, artifactFile)
            }
        }

        override fun close() {
            artifactFile.close()
        }
    }

    private inner class MetadataLibraryImpl(
        override val artifactContent: CompositeMetadataArtifact.ArtifactContent,
        override val sourceSet: CompositeMetadataArtifact.SourceSet,
        private val artifactFile: ArtifactFile
    ) : CompositeMetadataArtifact.MetadataLibrary {

        override val archiveExtension: String
            get() = kotlinProjectStructureMetadata.sourceSetBinaryLayout[sourceSet.sourceSetName]?.archiveExtension
                ?: SourceSetMetadataLayout.METADATA.archiveExtension

        override val checksum: Int
            get() = artifactFile.checksum

        override val checksumString: String
            get() = artifactFile.checksumString

        /**
         * Example:
         * org.jetbrains.sample-sampleLibrary-1.0.0-SNAPSHOT-appleAndLinuxMain-Vk5pxQ.klib
         */
        override val relativeFile: File = File(buildString {
            append(artifactContent.moduleDependencyIdentifier)
            append("-")
            append(artifactContent.moduleDependencyVersion)
            append("-")
            append(sourceSet.sourceSetName)
            append("-")
            append(checksumString)
            append(".")
            append(archiveExtension)
        })

        override fun copyTo(file: File): Boolean {
            require(file.extension == archiveExtension) {
                "Expected file.extension == '$archiveExtension'. Found ${file.extension}"
            }

            val libraryPath = "${sourceSet.sourceSetName}/"
            if (!artifactFile.containsDirectory(libraryPath)) return false
            file.parentFile.mkdirs()
            artifactFile.zip.copyPartially(file, libraryPath)

            return true
        }
    }

    private inner class CInteropMetadataLibraryImpl(
        override val artifactContent: CompositeMetadataArtifact.ArtifactContent,
        override val sourceSet: CompositeMetadataArtifact.SourceSet,
        override val cinteropLibraryName: String,
        private val artifactFile: ArtifactFile,
    ) : CompositeMetadataArtifact.CInteropMetadataLibrary {

        override val archiveExtension: String
            get() = SourceSetMetadataLayout.KLIB.archiveExtension

        override val checksum: Int
            get() = artifactFile.checksum

        override val checksumString: String
            get() = artifactFile.checksumString

        /**
         * Example:
         * org.jetbrains.sample-sampleLibrary-1.0.0-SNAPSHOT-appleAndLinuxMain-cinterop/
         *     org.jetbrains.sample_sampleLibrary-cinterop-simple-Vk5pxQ.klib
         */
        override val relativeFile: File = File(buildString {
            append(artifactContent.moduleDependencyIdentifier)
            append("-")
            append(artifactContent.moduleDependencyVersion)
            append("-")
            append(sourceSet.sourceSetName)
            append("-cinterop")
        }).resolve("$cinteropLibraryName-${checksumString}.${archiveExtension}")

        override fun copyTo(file: File): Boolean {
            require(file.extension == archiveExtension) {
                "Expected 'file.extension == '${SourceSetMetadataLayout.KLIB.archiveExtension}'. Found ${file.extension}"
            }

            val sourceSetName = sourceSet.sourceSetName
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

        companion object {
            val checksumStringEncoder: Encoder = Base64.getUrlEncoder().withoutPadding()
        }

        private var isClosed = false

        private val lazyZip = lazy {
            ensureNotClosed()
            ZipFile(file)
        }

        val zip: ZipFile get() = lazyZip.value

        val entries: List<ZipEntry> by lazy {
            zip.entries().toList()
        }

        val checksum: Int by lazy(LazyThreadSafetyMode.NONE) {
            val crc32 = CRC32()
            entries.forEach { entry -> crc32.update(entry.crc.toInt()) }
            crc32.value.toInt()
        }

        val checksumString: String by lazy(LazyThreadSafetyMode.NONE) {
            checksumStringEncoder.encodeToString(ByteBuffer.allocate(4).putInt(checksum).array())
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


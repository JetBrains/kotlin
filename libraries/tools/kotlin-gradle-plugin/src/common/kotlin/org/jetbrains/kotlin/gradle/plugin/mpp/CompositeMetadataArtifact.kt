/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import java.io.Closeable
import java.io.File

internal interface CompositeMetadataArtifact {

    interface Library {
        val artifactContent: ArtifactContent
        val sourceSet: SourceSet
        val archiveExtension: String
        val checksum: String

        /**
         * The proposed file-output path.
         * This can actually include several path parts if the [Library] requires additional scoping (e.g. [CInteropMetadataLibrary]s
         * need to be put into another folder)
         */
        val relativeFile: File

        /**
         * Copies the content of this [Library] directly into the given [file].
         * The [file] will be overwritten when it already exists.
         * Parent directories will be created if necessary.
         */
        fun copyTo(file: File): Boolean

        /**
         * Copies the content of this [Library] into the [directory] appending the [relativeFile] to it.
         * @see copyTo
         */
        fun copyIntoDirectory(directory: File) = copyTo(directory.resolve(relativeFile))
    }

    /**
     * Represents a Kotlin Metadata library produced by compiling the contained [sourceSet]
     */
    interface MetadataLibrary : Library

    /**
     * Represents a CInterop Metadata library, produced by the commonizer and attached to the [sourceSet]
     */
    interface CInteropMetadataLibrary : Library {
        val cinteropLibraryName: String
    }

    /**
     * Represents a SourceSet packaged into the [CompositeMetadataArtifact]
     */
    interface SourceSet {
        val artifactContent: ArtifactContent
        val sourceSetName: String
        val metadataLibrary: MetadataLibrary?
        val cinteropMetadataLibraries: List<CInteropMetadataLibrary>
    }

    interface ArtifactContent : Closeable {
        val moduleDependencyIdentifier: ModuleDependencyIdentifier
        val moduleDependencyVersion: String
        val sourceSets: List<SourceSet>
        fun getSourceSet(name: String): SourceSet
        fun findSourceSet(name: String): SourceSet?
    }

    val moduleDependencyIdentifier: ModuleDependencyIdentifier
    val moduleDependencyVersion: String

    fun open(): ArtifactContent

    fun <T> read(action: (artifactContent: ArtifactContent) -> T): T {
        return open().use(action)
    }
}

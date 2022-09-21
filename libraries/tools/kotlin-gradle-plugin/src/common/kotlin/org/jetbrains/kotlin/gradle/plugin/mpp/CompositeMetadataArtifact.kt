/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import java.io.Closeable
import java.io.File

internal interface CompositeMetadataArtifact {
    val moduleDependencyIdentifier: ModuleDependencyIdentifier
    val moduleDependencyVersion: String

    /**
     * Provides access to the actual content provided by this artifact.
     * Note: [CompositeMetadataArtifactContent] is [Closeable] and might actively open Files on access.
     * A [Closeable.close] call is required.
     *
     * Alternatively use the [read] function instead.
     */
    fun open(): CompositeMetadataArtifactContent

    /**
     * Safe shortcut function for opening and reading the content of this artifact.
     * The [CompositeMetadataArtifactContent] will be closed after the [action] executed.
     */
    fun <T> read(action: (artifactContent: CompositeMetadataArtifactContent) -> T): T {
        return open().use(action)
    }
}

internal interface CompositeMetadataArtifactContent : Closeable {
    /**
     * Back reference to the [CompositeMetadataArtifact] that opened this [CompositeMetadataArtifactContent]
     */
    val containingArtifact: CompositeMetadataArtifact
    val sourceSets: List<SourceSetContent>
    fun getSourceSet(name: String): SourceSetContent
    fun findSourceSet(name: String): SourceSetContent?

    /**
     * Represents a SourceSet packaged into the [CompositeMetadataArtifact]
     */
    interface SourceSetContent {
        /**
         * Back reference to the [CompositeMetadataArtifactContent] that contains this [SourceSetContent]
         */
        val containingArtifactContent: CompositeMetadataArtifactContent
        val sourceSetName: String
        val metadataBinary: MetadataBinary?
        val cinteropMetadataBinaries: List<CInteropMetadataBinary>
    }


    interface Binary {
        /**
         * Back reference to the [SourceSetContent] that contains this [Binary]
         */
        val containingSourceSetContent: SourceSetContent

        val archiveExtension: String

        /**
         * The proposed file-output path.
         * This can actually include several path parts if the [Binary] requires additional scoping (e.g. [CInteropMetadataBinary]s
         * need to be put into another folder)
         */
        val relativeFile: File

        val checksum: String

        /**
         * Copies the content of this [Binary] directly into the given [file].
         * The [file] will be overwritten when it already exists.
         * Parent directories will be created if necessary.
         */
        fun copyTo(file: File): Boolean

        /**
         * Copies the content of this [Binary] into the [directory] appending the [relativeFile] to it.
         * @see copyTo
         */
        fun copyIntoDirectory(directory: File) = copyTo(directory.resolve(relativeFile))
    }


    /**
     * Represents a Kotlin Metadata Binary produced by compiling the contained [containingSourceSetContent]
     */
    interface MetadataBinary : Binary

    /**
     * Represents a CInterop Metadata Binary, produced by the commonizer and attached to the [containingSourceSetContent]
     */
    interface CInteropMetadataBinary : Binary {
        val cinteropLibraryName: String
    }
}
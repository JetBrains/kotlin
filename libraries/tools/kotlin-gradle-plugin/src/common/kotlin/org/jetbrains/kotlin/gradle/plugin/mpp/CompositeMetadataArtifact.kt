/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.plugin.mpp.CompositeMetadataArtifactContent.SourceSetContent
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


/**
 * This [CompositeMetadataArtifactContent] abstraction provides access into a metadata artifact published by a Multiplatform Library
 *
 * ### Example for the kotlinx.coroutines.core library
 * During publication, kotlinx.coroutines.core will compile all its shared SourceSets individually into "kotlin metadata klib"s.
 * e.g. a task called compileCommonMainKotlinMetadata will take the 'commonMain' SourceSet of the coroutines library
 * and will produce a 'commonMain.klib' library which will contain only "Kotlin Metadata" (Signatures w/o any function bodies).
 * Such klibs will be produced for all 'shared SourceSets'.
 *
 * During publication, all of those individual SourceSet klib binaries will be packaged into a 'jar' file called
 * the Composite Metadata Artifact. All non-host specific klibs will be packaged and published in the root publication without target
 * classifier (e.g. kotlinx-coroutines-core:
 * https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/1.6.4/kotlinx-coroutines-core-1.6.4-all.jar
 *
 * In the case of kotlinx.coroutines.core there will be [sourceSets] named 'commonMain', 'nativeMain', ....
 * The commonMain.klib can therefore be accessed through the [SourceSetContent] of the
 * 'commonMain' source set inside this [CompositeMetadataArtifactContent]
 *
 * e.g. copy the metadata klib of the 'commonMain' SourceSet into 'myFile'
 * ```kotlin
 * artifact.read { artifactContent ->
 *     artifactContent.getSourceSet("commonMain").metadataBinary?.copyTo(myFile)
 * }
 * ```
 */
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
/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.utils.keysToMap
import java.io.File

/**
 * Representation of the uklib used in the GMT
 */
internal class UklibCompositeMetadataArtifact(
    val moduleVersion: ModuleVersionIdentifier,
    val allVisibleFragments: List<UklibFragment>,
) : CompositeMetadataArtifact {

    override val moduleDependencyIdentifier: ModuleDependencyIdentifier
        get() = ModuleDependencyIdentifier(moduleVersion.group, moduleVersion.name)
    override val moduleDependencyVersion: String
        get() = moduleVersion.version

    override fun open(): CompositeMetadataArtifactContent {
        return UklibCompositeMetadataArtifactContent(
            this,
            moduleVersion,
            allVisibleFragments,
        )
    }

    override fun exists(): Boolean = true
}

internal class UklibCompositeMetadataArtifactContent(
    override val containingArtifact: CompositeMetadataArtifact,
    val moduleVersion: ModuleVersionIdentifier,
    allVisibleFragments: List<UklibFragment>,
) : CompositeMetadataArtifactContent {

    private val fragmentSourceSets: Map<String, CompositeMetadataArtifactContent.SourceSetContent> =
        allVisibleFragments.keysToMap { fragment ->
            UklibCompositeMetadataArtifactSourceSetContent(
                this,
                moduleVersion,
                fragment,
            )
        }.mapKeys { it.key.identifier }

    override val sourceSets: List<CompositeMetadataArtifactContent.SourceSetContent>
        get() = fragmentSourceSets.values.toList()

    override fun getSourceSet(name: String): CompositeMetadataArtifactContent.SourceSetContent {
        return fragmentSourceSets[name]!!
    }

    override fun findSourceSet(name: String): CompositeMetadataArtifactContent.SourceSetContent? {
        return fragmentSourceSets[name]
    }

    override fun close() {
        /**
         * Uklib is unzipped by the transform and there are no resources to close
         */
    }
}

internal class UklibCompositeMetadataArtifactSourceSetContent(
    override val containingArtifactContent: CompositeMetadataArtifactContent,
    val moduleVersion: ModuleVersionIdentifier,
    val fragment: UklibFragment,
) : CompositeMetadataArtifactContent.SourceSetContent {
    override val sourceSetName: String
        get() = fragment.identifier

    override val metadataBinary: CompositeMetadataArtifactContent.MetadataBinary
        get() = UklibCompositeMetadataBinary(this, moduleVersion, fragment)

    override val cinteropMetadataBinaries: List<CompositeMetadataArtifactContent.CInteropMetadataBinary>
        get() = emptyList()
}

internal class UklibCompositeMetadataBinary(
    override val containingSourceSetContent: CompositeMetadataArtifactContent.SourceSetContent,
    val moduleVersion: ModuleVersionIdentifier,
    val fragment: UklibFragment,
) : CompositeMetadataArtifactContent.MetadataBinary {
    override val archiveExtension: String
        get() = ""

    // This needs to be unique per fragment
    // FIXME: Do we need a checksum here?
    override val relativeFile: File
        get() = File("uklib-${moduleVersion.group}-${moduleVersion.name}-${moduleVersion.version}-${fragment.identifier}")
    override val checksum: String
        // FIXME: Why does this even exist separately?
        get() = ""

    override fun copyTo(file: File): Boolean {
        return fragment.file().copyRecursively(
            file,
            overwrite = true,
        )
    }
}
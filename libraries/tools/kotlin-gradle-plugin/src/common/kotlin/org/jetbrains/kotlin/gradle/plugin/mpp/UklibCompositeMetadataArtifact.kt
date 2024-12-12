/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.kotlin.gradle.utils.checksumString
import java.io.File
import java.security.MessageDigest

/**
 * Representation of the uklib used in the GMT
 */
internal class UklibCompositeMetadataArtifact(
    val moduleId: ModuleId,
    val allVisibleFragments: List<UklibFragment>,
) : CompositeMetadataArtifact {
    data class ModuleId(val group: String, val name: String, val version: String)

    override val moduleDependencyIdentifier: ModuleDependencyIdentifier
        get() = ModuleDependencyIdentifier(moduleId.group, moduleId.name)
    override val moduleDependencyVersion: String
        get() = moduleId.version

    override fun open(): CompositeMetadataArtifactContent {
        return UklibCompositeMetadataArtifactContent(
            this,
            moduleId,
            allVisibleFragments,
        )
    }

    override fun exists(): Boolean = true
}

internal class UklibCompositeMetadataArtifactContent(
    override val containingArtifact: CompositeMetadataArtifact,
    val moduleId: UklibCompositeMetadataArtifact.ModuleId,
    allVisibleFragments: List<UklibFragment>,
) : CompositeMetadataArtifactContent {

    private val fragmentSourceSets: Map<String, CompositeMetadataArtifactContent.SourceSetContent> =
        allVisibleFragments.keysToMap { fragment ->
            UklibCompositeMetadataArtifactSourceSetContent(
                this,
                moduleId,
                fragment,
            )
        }.mapKeys { it.key.identifier }

    override val sourceSets: List<CompositeMetadataArtifactContent.SourceSetContent>
        get() = fragmentSourceSets.values.toList()

    override fun getSourceSet(name: String): CompositeMetadataArtifactContent.SourceSetContent {
        return fragmentSourceSets[name] ?: error("Missing uklib fragment $name in $moduleId")
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
    val moduleId: UklibCompositeMetadataArtifact.ModuleId,
    val fragment: UklibFragment,
) : CompositeMetadataArtifactContent.SourceSetContent {
    override val sourceSetName: String
        get() = fragment.identifier

    override val metadataBinary: CompositeMetadataArtifactContent.MetadataBinary
        get() = UklibCompositeMetadataBinary(this, moduleId, fragment)

    override val cinteropMetadataBinaries: List<CompositeMetadataArtifactContent.CInteropMetadataBinary>
        get() = emptyList()
}

internal class UklibCompositeMetadataBinary(
    override val containingSourceSetContent: CompositeMetadataArtifactContent.SourceSetContent,
    val moduleId: UklibCompositeMetadataArtifact.ModuleId,
    val fragment: UklibFragment,
) : CompositeMetadataArtifactContent.MetadataBinary {
    override val archiveExtension: String
        get() = ""
    override val relativeFile: File
        get() = File("uklib-${moduleId.group}-${moduleId.name}-${moduleId.version}-${fragment.identifier}-${checksum}")

    // Rely on unique transform path
    override val checksum: String
        get() = md5.digest(fragment.file().path.encodeToByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    override fun copyTo(file: File): Boolean {
        return fragment.file().copyRecursively(
            file,
            overwrite = true,
        )
    }

    private companion object {
        val md5 = MessageDigest.getInstance("MD5")
    }
}
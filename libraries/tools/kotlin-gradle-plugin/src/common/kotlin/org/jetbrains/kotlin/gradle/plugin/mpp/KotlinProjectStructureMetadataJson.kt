/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import kotlinx.serialization.Serializable

@Serializable
internal data class KotlinProjectStructureMetadataJson(
    val projectStructure: KotlinProjectStructureMetadataNodeJson,
)

@Serializable
internal data class KotlinProjectStructureMetadataNodeJson(
    val formatVersion: String,
    val isPublishedAsRoot: String,
    val variants: List<VariantWrapperJson>,
    val sourceSets: List<SourceSetWrapperJson>,
)

@Serializable
internal data class VariantWrapperJson(
    val variant: VariantNodeJson,
)

@Serializable
internal data class VariantNodeJson(
    val name: String,
    val sourceSet: List<String> = emptyList(),
)

@Serializable
internal data class SourceSetWrapperJson(
    val sourceSet: SourceSetNodeJson,
)

@Serializable
internal data class SourceSetNodeJson(
    val name: String,
    val dependsOn: List<String> = emptyList(),
    val moduleDependency: List<String> = emptyList(),
    val sourceSetCInteropMetadataDirectory: String? = null,
    val binaryLayout: String? = null,
    val hostSpecific: String? = null,
)

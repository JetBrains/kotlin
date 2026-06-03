/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TransformedMetadataLibraryRecordJson(
    val moduleId: KmpModuleIdentifierJson,
    val file: String,
    val sourceSetName: String? = null,
)

@Serializable
internal data class KmpModuleIdentifierJson(
    val groupAndName: GradleGroupAndNameJson? = null,
    val componentId: GradleComponentIdJson,
)

@Serializable
internal data class GradleGroupAndNameJson(
    val group: String,
    val name: String,
)

/**
 * Polymorphic JSON representation of [KmpModuleIdentifier.GradleComponentId].
 * Uses the default `"type"` discriminator field, matching the legacy Gson wire format.
 */
@Serializable
internal sealed class GradleComponentIdJson {
    @Serializable
    @SerialName("module")
    data class ModuleComponentIdJson(
        val group: String,
        val name: String,
    ) : GradleComponentIdJson()

    @Serializable
    @SerialName("project")
    data class ProjectComponentIdJson(
        val projectPath: String,
        val buildPath: String,
    ) : GradleComponentIdJson()
}

internal fun TransformedMetadataLibraryRecord.toJson(): TransformedMetadataLibraryRecordJson =
    TransformedMetadataLibraryRecordJson(
        moduleId = KmpModuleIdentifierJson(
            groupAndName = moduleId.groupAndName?.let { GradleGroupAndNameJson(it.group, it.name) },
            componentId = when (val id = moduleId.componentId) {
                is KmpModuleIdentifier.ModuleComponentId -> GradleComponentIdJson.ModuleComponentIdJson(
                    group = id.group,
                    name = id.name,
                )
                is KmpModuleIdentifier.ProjectComponentId -> GradleComponentIdJson.ProjectComponentIdJson(
                    projectPath = id.projectPath,
                    buildPath = id.buildPath,
                )
            }
        ),
        file = file,
        sourceSetName = sourceSetName,
    )

internal fun TransformedMetadataLibraryRecordJson.toRecord(): TransformedMetadataLibraryRecord =
    TransformedMetadataLibraryRecord(
        moduleId = KmpModuleIdentifier(
            groupAndName = moduleId.groupAndName?.let { KmpModuleIdentifier.GradleGroupAndName(it.group, it.name) },
            componentId = when (val id = moduleId.componentId) {
                is GradleComponentIdJson.ModuleComponentIdJson -> KmpModuleIdentifier.ModuleComponentId(
                    group = id.group,
                    name = id.name,
                )
                is GradleComponentIdJson.ProjectComponentIdJson -> KmpModuleIdentifier.ProjectComponentId(
                    projectPath = id.projectPath,
                    buildPath = id.buildPath,
                )
            }
        ),
        file = file,
        sourceSetName = sourceSetName,
    )

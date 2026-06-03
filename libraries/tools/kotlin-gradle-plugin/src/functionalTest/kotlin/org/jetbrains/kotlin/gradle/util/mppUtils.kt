package org.jetbrains.kotlin.gradle.util

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinMetadataCompilations
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinProjectStructureMetadata
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.jetbrains.kotlin.gradle.internal.json.AbsoluteFileSerializer
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.plugin.mpp.toJson
import org.jetbrains.kotlin.gradle.targets.metadata.locateOrRegisterGenerateProjectStructureMetadataTask

/**
 * During normal build the task `generateProjectStructureMetadata` generates `kotlin-project-structure-metadata.json` and `source-sets-metadata.json`
 * But with functional tests, we don't have a Gradle execution phase, so we need to manually put psm and source sets data into the task outputs.
 */
internal fun ProjectInternal.mockGenerateProjectStructureMetadataTaskOutputs() {
    locateOrRegisterGenerateProjectStructureMetadataTask().get()
        .resultFile.also { it.parentFile.mkdirs() }.writeText(multiplatformExtension.kotlinProjectStructureMetadata.toJson())

    launch {
        val sourceSetOutputs = multiplatformExtension.kotlinMetadataCompilations()
            .associate { it.defaultSourceSet.name to it.output.classesDirs.singleFile }

        locateOrRegisterGenerateProjectStructureMetadataTask().get()
            .sourceSetMetadataOutputsFile.get().asFile.also { it.parentFile.mkdirs() }
            .writeText(KgpJson.default.encodeToString(MapSerializer(String.serializer(), AbsoluteFileSerializer), sourceSetOutputs))
    }
}

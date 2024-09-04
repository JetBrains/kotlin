/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.JsonUtils
import org.jetbrains.kotlin.gradle.utils.buildPathCompat
import org.jetbrains.kotlin.gradle.utils.currentBuildId
import java.io.File

/**
 * Exported KMP Coordinates to be consumed by other KMP projects
 * for example, in Kotlin Project Structure Metadata or POM Dependencies rewriter
 */
internal data class KotlinProjectCoordinatesData(
    @get:Input
    val buildPath: String,

    @get:Input
    val projectPath: String,

    @get:Nested
    val moduleId: ModuleDependencyIdentifier,
)

private const val KOTLIN_PROJECT_COORDINATES_DATA_FILE_NAME = "kotlinProjectCoordinates.json"

internal fun parseKotlinProjectCoordinatesOrNull(file: File): KotlinProjectCoordinatesData? {
    if (file.name != KOTLIN_PROJECT_COORDINATES_DATA_FILE_NAME) return null
    return JsonUtils.gson.fromJson(file.readText(), KotlinProjectCoordinatesData::class.java)
}

internal val ExportKotlinProjectCoordinates = KotlinProjectSetupCoroutine {
    val task = project.tasks.register("exportKotlinProjectCoordinates", ExportKotlinProjectCoordinatesTask::class.java) { task ->
        task.outputJsonFile.set(project.layout.buildDirectory.file("internal/kmp/$KOTLIN_PROJECT_COORDINATES_DATA_FILE_NAME"))
        launch {
            val coordinates = collectKotlinProjectCoordinates()
            task.data.set(coordinates)
        }
    }
    val apiElements = project.configurations.getByName(project.multiplatformExtension.awaitMetadataTarget().apiElementsConfigurationName)
    apiElements.outgoing.variants.create("kotlin-project-coordinates") { variant ->
        variant.artifact(task.map { it.outputJsonFile })
        variant.attributes.attributeProvider(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, provider { "kotlin-project-coordinates" })
    }
}

internal fun InternalKotlinSourceSet.projectCoordinatesConfiguration() =
    LazyResolvedConfiguration(
        resolvableMetadataConfiguration,
        { attributeContainer ->
            attributeContainer.attributeProvider(
                ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                project.provider { "kotlin-project-coordinates" })
        }
    )

private suspend fun Project.collectKotlinProjectCoordinates(): KotlinProjectCoordinatesData {
    return KotlinProjectCoordinatesData(
        buildPath = project.currentBuildId().buildPathCompat,
        projectPath = project.path,
        moduleId = ModuleIds.idOfRootModuleSafe(this)
    )
}

@DisableCachingByDefault(because = "Not worth caching")
internal abstract class ExportKotlinProjectCoordinatesTask : DefaultTask() {

    @get:OutputFile
    abstract val outputJsonFile: RegularFileProperty

    @get:Nested
    abstract val data: Property<KotlinProjectCoordinatesData>

    @TaskAction
    fun action() {
        val file = outputJsonFile.get().asFile
        val json = JsonUtils.gson.toJson(data.get())
        file.writeText(json)
    }
}

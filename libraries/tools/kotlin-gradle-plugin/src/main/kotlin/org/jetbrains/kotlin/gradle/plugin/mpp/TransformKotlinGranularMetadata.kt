/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.API_SCOPE
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.IMPLEMENTATION_SCOPE
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.targets.metadata.ALL_COMPILE_METADATA_CONFIGURATION_NAME
import java.io.File
import javax.inject.Inject

open class TransformKotlinGranularMetadata
@Inject constructor(
    @get:Internal
    val kotlinSourceSet: KotlinSourceSet
) : DefaultTask() {

    @get:OutputDirectory
    val outputsDir: File = project.buildDir.resolve("kotlinSourceSetMetadata/${kotlinSourceSet.name}")

    @Suppress("unused") // Gradle input
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val allSourceSetsMetadataConfiguration = project.configurations.getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME)

    private val participatingSourceSets: Set<KotlinSourceSet>
        get() = transformation.kotlinSourceSet.getSourceSetHierarchy().toMutableSet().apply {
            if (any { it.name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME })
                add(project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME))
        }

    @Suppress("unused") // Gradle input
    @get:Input
    internal val inputSourceSetsAndCompilations: Map<String, Iterable<String>>
        get() {
            val sourceSets = participatingSourceSets
            return CompilationSourceSetUtil.compilationsBySourceSets(project)
                .filterKeys { it in sourceSets }
                .entries.associate { (sourceSet, compilations) ->
                    sourceSet.name to compilations.map { it.name }.sorted()
                }
        }

    private val participatingCompilations: Iterable<KotlinCompilation<*>>
        get() {
            val sourceSets = participatingSourceSets
            return CompilationSourceSetUtil.compilationsBySourceSets(project).filterKeys { it in sourceSets }.values.flatten()
        }

    @Suppress("unused") // Gradle input
    @get:Input
    internal val inputCompilationDependencies: Map<String, Set<List<String?>>>
        get() = participatingCompilations.associate {
            it.name to project.configurations.getByName(it.compileDependencyConfigurationName)
                .allDependencies.map { listOf(it.group, it.name, it.version) }.toSet()
        }

    private val transformation =
        GranularMetadataTransformation(
            project,
            kotlinSourceSet,
            listOf(API_SCOPE, IMPLEMENTATION_SCOPE),
            listOf(project.configurations.getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME))
        )

    @get:Internal
    internal val metadataDependencyResolutions: Iterable<MetadataDependencyResolution>
        get() = transformation.metadataDependencyResolutions

    @get:Internal
    internal val filesByResolution: Map<out MetadataDependencyResolution, FileCollection>
        get() = metadataDependencyResolutions.filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .associate { it to project.files(it.getMetadataFilesBySourceSet(outputsDir, doProcessFiles = false).values) }

    @TaskAction
    fun transformMetadata() {
        if (outputsDir.isDirectory) {
            outputsDir.deleteRecursively()
        }
        outputsDir.mkdirs()

        metadataDependencyResolutions
            .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .forEach { it.getMetadataFilesBySourceSet(outputsDir, doProcessFiles = true) }
    }
}
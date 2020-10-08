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
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.*
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.targets.metadata.ALL_COMPILE_METADATA_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetConfigurator
import org.jetbrains.kotlin.gradle.utils.getValue
import java.io.File
import javax.inject.Inject

open class TransformKotlinGranularMetadata
@Inject constructor(
    @get:Internal
    @field:Transient
    val kotlinSourceSet: KotlinSourceSet
) : DefaultTask() {

    @get:OutputDirectory
    val outputsDir: File by project.provider {
        project.buildDir.resolve("kotlinSourceSetMetadata/${kotlinSourceSet.name}")
    }

    @Suppress("unused") // Gradle input
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val allSourceSetsMetadataConfiguration: FileCollection by lazy {
        project.files(project.configurations.getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME))
    }

    private val participatingSourceSets: Set<KotlinSourceSet>
        get() = transformation.kotlinSourceSet.getSourceSetHierarchy().toMutableSet().apply {
            if (any { it.name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME })
                add(project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME))
        }

    @Suppress("unused") // Gradle input
    @get:Input
    internal val inputSourceSetsAndCompilations: Map<String, Iterable<String>> by project.provider {
        val sourceSets = participatingSourceSets
        CompilationSourceSetUtil.compilationsBySourceSets(project)
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
    internal val inputCompilationDependencies: Map<String, Set<List<String?>>> by project.provider {
        participatingCompilations.associate {
            it.name to project.configurations.getByName(it.compileDependencyConfigurationName)
                .allDependencies.map { listOf(it.group, it.name, it.version) }.toSet()
        }
    }

    @get:Internal
    @delegate:Transient
    internal val transformation: GranularMetadataTransformation by lazy {
        GranularMetadataTransformation(
            project,
            kotlinSourceSet,
            listOf(API_SCOPE, IMPLEMENTATION_SCOPE, COMPILE_ONLY_SCOPE),
            project.configurations.getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME),
            lazy {
                KotlinMetadataTargetConfigurator.dependsOnWithInterCompilationDependencies(project, kotlinSourceSet).map {
                    project.tasks.withType(TransformKotlinGranularMetadata::class.java)
                        .getByName(KotlinMetadataTargetConfigurator.transformGranularMetadataTaskName(it.name))
                        .transformation
                }
            }
        )
    }

    @get:Internal
    @delegate:Transient // exclude from Gradle instant execution state
    internal val metadataDependencyResolutions: Iterable<MetadataDependencyResolution> by project.provider {
        transformation.metadataDependencyResolutions
    }

    private val extractableFilesByResolution: Map<out MetadataDependencyResolution, ExtractableMetadataFiles>
        get() = metadataDependencyResolutions
            .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .associate { it to it.getExtractableMetadataFiles(outputsDir) }

    @get:Internal
    internal val filesByResolution: Map<out MetadataDependencyResolution, FileCollection>
        get() = extractableFilesByResolution.mapValues { (_, value) ->
            project.files(value.getMetadataFilesPerSourceSet(false).values)
        }

    private val extractableFiles by project.provider { extractableFilesByResolution.values }

    @TaskAction
    fun transformMetadata() {
        if (outputsDir.isDirectory) {
            outputsDir.deleteRecursively()
        }
        outputsDir.mkdirs()

        extractableFiles.forEach { it.getMetadataFilesPerSourceSet(doProcessFiles = true) }
    }
}
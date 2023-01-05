/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.*
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.ALL_COMPILE_METADATA_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.metadata.ResolvedMetadataFilesProvider
import org.jetbrains.kotlin.gradle.targets.metadata.dependsOnClosureWithInterCompilationDependencies
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.notCompatibleWithConfigurationCacheCompat
import org.jetbrains.kotlin.gradle.utils.outputFilesProvider
import java.io.File
import javax.inject.Inject

/* Keep typealias for source compatibility */
@Suppress("unused")
@Deprecated("Task was renamed to MetadataDependencyTransformationTask", replaceWith = ReplaceWith("MetadataDependencyTransformationTask"))
typealias TransformKotlinGranularMetadata = MetadataDependencyTransformationTask

open class MetadataDependencyTransformationTask
@Inject constructor(
    @get:Internal
    @field:Transient
    val kotlinSourceSet: KotlinSourceSet
) : DefaultTask() {

    init {
        notCompatibleWithConfigurationCacheCompat(
            "Task $name does not support Gradle Configuration Cache. Check KT-49933 for more info"
        )
    }

    @get:OutputDirectory
    val outputsDir: File by project.provider {
        project.kotlinTransformedMetadataLibraryDirectoryForBuild(kotlinSourceSet.name)
    }

    @Suppress("unused") // Gradle input
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    internal val configurationToResolve: FileCollection get() = kotlinSourceSet.internal.resolvableMetadataConfiguration

    private val participatingSourceSets: Set<KotlinSourceSet>
        get() = transformation.kotlinSourceSet.internal.withDependsOnClosure.toMutableSet().apply {
            if (any { it.name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME })
                add(project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME))
        }

    @Suppress("unused") // Gradle input
    @get:Input
    internal val inputSourceSetsAndCompilations: Map<String, Iterable<String>> by project.provider {
        participatingSourceSets.associate { sourceSet ->
            sourceSet.name to sourceSet.internal.compilations.map { it.name }.sorted()
        }
    }

    private val participatingCompilations: Iterable<KotlinCompilation<*>>
        get() = participatingSourceSets.flatMap { it.internal.compilations }.toSet()

    @Suppress("unused") // Gradle input
    @get:Input
    internal val inputCompilationDependencies: Map<String, Set<List<String?>>> by project.provider {
        participatingCompilations.associate {
            it.name to project.configurations.getByName(it.compileDependencyConfigurationName)
                .allDependencies.map { listOf(it.group, it.name, it.version) }.toSet()
        }
    }

    private val transformation: GranularMetadataTransformation by lazy {
        GranularMetadataTransformation(
            project,
            kotlinSourceSet,
            lazy {
                dependsOnClosureWithInterCompilationDependencies(kotlinSourceSet).map {
                    project.tasks.withType(MetadataDependencyTransformationTask::class.java)
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

    /**
     * Contains "actually transformed" metadata libraries (extracted from any [CompositeMetadataArtifact]) as well as
     * [FileCollection]s pointing to project dependency klibs.
     *
     * Project dependencies are still required to be listed here (despite not being produced by this task), since
     * this [filesByResolution] will be used to build the compile-classpath for metadata compilations.
     */
    @get:Internal
    internal val filesByResolution: Map<out MetadataDependencyResolution, FileCollection>
        get() = metadataDependencyResolutions
            .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .associateWith { chooseVisibleSourceSets ->
                outputFilesProvider {
                    project.transformMetadataLibrariesForBuild(
                        chooseVisibleSourceSets, outputsDir, materializeFiles = false
                    )
                }
            }

    @TaskAction
    fun transformMetadata() {
        if (outputsDir.isDirectory) {
            outputsDir.deleteRecursively()
        }
        outputsDir.mkdirs()

        metadataDependencyResolutions
            .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .forEach { chooseVisibleSourceSets ->
                project.transformMetadataLibrariesForBuild(chooseVisibleSourceSets, outputsDir, true)
            }
    }
}

internal class SourceSetResolvedMetadataProvider(
    taskProvider: TaskProvider<out MetadataDependencyTransformationTask>
) : ResolvedMetadataFilesProvider {
    override val buildDependencies: Iterable<TaskProvider<*>> = listOf(taskProvider)
    override val metadataResolutions: Iterable<MetadataDependencyResolution> by taskProvider.map { it.metadataDependencyResolutions }
    override val metadataFilesByResolution: Map<out MetadataDependencyResolution, FileCollection>
            by taskProvider.map { it.filesByResolution }
}

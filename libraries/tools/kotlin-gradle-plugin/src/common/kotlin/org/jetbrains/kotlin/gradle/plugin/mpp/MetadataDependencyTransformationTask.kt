/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.*
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.metadata.dependsOnClosureWithInterCompilationDependencies
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.*
import java.io.File
import javax.inject.Inject

/* Keep typealias for source compatibility */
@Suppress("unused")
@Deprecated("Task was renamed to MetadataDependencyTransformationTask", replaceWith = ReplaceWith("MetadataDependencyTransformationTask"))
typealias TransformKotlinGranularMetadata = MetadataDependencyTransformationTask

open class MetadataDependencyTransformationTask
@Inject constructor(
    kotlinSourceSet: KotlinSourceSet,
    private val objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout
) : DefaultTask() {

    //region Task Configuration State & Inputs
    private val transformationParameters = GranularMetadataTransformation.Params(project, kotlinSourceSet)

    @get:OutputDirectory
    internal val outputsDir: File get() = projectLayout.kotlinTransformedMetadataLibraryDirectoryForBuild(transformationParameters.sourceSetName)

    @Suppress("unused") // Gradle input
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    internal val configurationToResolve: FileCollection = kotlinSourceSet.internal.resolvableMetadataConfiguration

    @Transient // Only needed for configuring task inputs
    private val participatingSourceSetsLazy: Lazy<Set<KotlinSourceSet>>? = lazy {
        kotlinSourceSet.internal.withDependsOnClosure.toMutableSet().apply {
            if (any { it.name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME })
                add(project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME))
        }
    }

    @Transient // Only needed for configuring task inputs
    private val parentTransformationTasksLazy: Lazy<List<TaskProvider<MetadataDependencyTransformationTask>>>? = lazy {
        dependsOnClosureWithInterCompilationDependencies(kotlinSourceSet).mapNotNull {
            project
                .tasks
                .locateTask(KotlinMetadataTargetConfigurator.transformGranularMetadataTaskName(it.name))
        }
    }

    private val participatingSourceSets: Set<KotlinSourceSet> get() = participatingSourceSetsLazy?.value
        ?: error("`participatingSourceSets` is null. " +
                 "Probably it is accessed it during Task Execution with state loaded from Configuration Cache")

    private val parentTransformationTasks: List<TaskProvider<MetadataDependencyTransformationTask>> get() =
        parentTransformationTasksLazy?.value
            ?: error("`parentTransformationTasks` is null. " +
                     "Probably it is accessed it during Task Execution with state loaded from Configuration Cache")

    @Suppress("unused") // Gradle input
    @get:Input
    internal val inputSourceSetsAndCompilations: Map<String, Iterable<String>> by lazy {
        participatingSourceSets.associate { sourceSet ->
            sourceSet.name to sourceSet.internal.compilations.map { it.name }.sorted()
        }
    }

    @Suppress("unused") // Gradle input
    @get:Input
    internal val inputCompilationDependencies: Map<String, Set<List<String?>>> by lazy {
        participatingSourceSets.flatMap { it.internal.compilations }.associate {
            it.name to project.configurations.getByName(it.compileDependencyConfigurationName)
                .allDependencies.map { listOf(it.group, it.name, it.version) }.toSet()
        }
    }

    @get:OutputFile
    internal val transformedLibrariesFileIndex: RegularFileProperty = objectFactory
        .fileProperty()
        .apply { set(outputsDir.resolve("${kotlinSourceSet.name}.transformedLibraries")) }

    @get:OutputFile
    internal val visibleSourceSetsFile: RegularFileProperty = objectFactory
        .fileProperty()
        .apply { set(outputsDir.resolve("${kotlinSourceSet.name}.visibleSourceSets")) }

    @get:InputFiles
    internal val parentVisibleSourceSetFiles: FileCollection = project.filesProvider {
        parentTransformationTasks.map { taskProvider ->
            taskProvider.flatMap { task ->
                task.visibleSourceSetsFile.map { it.asFile }
            }
        }
    }

    @get:InputFiles
    internal val parentTransformedLibraries: FileCollection = project.filesProvider {
        parentTransformationTasks.map { taskProvider ->
            taskProvider.map { task -> task.ownTransformedLibraries }
        }
    }

    //endregion Task Configuration State & Inputs

    @TaskAction
    fun transformMetadata() {
        val transformation = GranularMetadataTransformation(
            params = transformationParameters,
            visibleSourceSetsFromParentsProvider = { parentVisibleSourceSetFiles.map(::readVisibleSourceSetsFile) }
        )

        if (outputsDir.isDirectory) {
            outputsDir.deleteRecursively()
        }
        outputsDir.mkdirs()

        val metadataDependencyResolutions = transformation.metadataDependencyResolutions

        val transformedLibraries = metadataDependencyResolutions
            .flatMap { resolution ->
                when (resolution) {
                    is MetadataDependencyResolution.ChooseVisibleSourceSets ->
                        objectFactory.transformMetadataLibrariesForBuild(resolution, outputsDir, true)
                    is MetadataDependencyResolution.KeepOriginalDependency ->
                        transformationParameters.resolvedMetadataConfiguration.getArtifacts(resolution.dependency).map { it.file }
                    is MetadataDependencyResolution.Exclude -> emptyList()
                }
            }

        writeTransformedLibraries(transformedLibraries)
        writeVisibleSourceSets(transformation.visibleSourceSetsByComponentId)
    }

    private fun writeTransformedLibraries(files: List<File>) {
        val content = files.joinToString(System.lineSeparator())
        transformedLibrariesFileIndex.get().asFile.writeText(content)
    }

    private fun writeVisibleSourceSets(visibleSourceSetsByComponentId: Map<String, Set<String>>) {
        val content = visibleSourceSetsByComponentId.entries.joinToString("\n") { (id, visibleSourceSets) ->
            "$id => ${visibleSourceSets.joinToString(",")}"
        }
        visibleSourceSetsFile.get().asFile.writeText(content)
    }

    private fun readVisibleSourceSetsFile(file: File): Map<String, Set<String>> = file
        .readLines()
        .associate { string ->
            val (id, visibleSourceSetsString) = string.split(" => ")
            id to visibleSourceSetsString.split(",").toSet()
        }

    @get:Internal // Warning! ownTransformedLibraries is available only after Task Execution
    internal val ownTransformedLibraries: FileCollection = project.filesProvider {
        transformedLibrariesFileIndex.map { regularFile ->
            regularFile.asFile.readLines().map { File(it) }
        }
    }

    @get:Internal // Warning! allTransformedLibraries is available only after Task Execution
    val allTransformedLibraries: FileCollection get() = ownTransformedLibraries + parentTransformedLibraries
}
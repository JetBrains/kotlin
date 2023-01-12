/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
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
    val outputsDir: File get() = projectLayout.kotlinTransformedMetadataLibraryDirectoryForBuild(transformationParameters.sourceSetName)

    @Suppress("unused") // Gradle input
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    internal val configurationToResolve: FileCollection = kotlinSourceSet.internal.resolvableMetadataConfiguration

    @delegate:Transient // Only needed for configuring task inputs
    private val participatingSourceSets: Set<KotlinSourceSet> by lazy {
        kotlinSourceSet.internal.withDependsOnClosure.toMutableSet().apply {
            if (any { it.name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME })
                add(project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME))
        }
    }

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
    val transformedLibrariesFileIndex: RegularFileProperty = objectFactory
        .fileProperty()
        .apply { set(outputsDir.resolve("${kotlinSourceSet.name}.transformedLibraries")) }

    @get:OutputFile
    val visibleSourceSetsFile: RegularFileProperty = objectFactory
        .fileProperty()
        .apply { set(outputsDir.resolve("${kotlinSourceSet.name}.visibleSourceSets")) }

    @get:InputFiles
    val parentVisibleSourceSetFiles: ConfigurableFileCollection = objectFactory
        .fileCollection()
        .from(
            {
                val parentSourceSets: List<Provider<File>> = dependsOnClosureWithInterCompilationDependencies(kotlinSourceSet).mapNotNull {
                    project
                        .tasks
                        .locateTask<MetadataDependencyTransformationTask>(KotlinMetadataTargetConfigurator.transformGranularMetadataTaskName(it.name))
                        ?.flatMap { it.visibleSourceSetsFile.map { it.asFile } }
                }

                parentSourceSets
            }
        )

    //endregion Task Configuration State & Inputs

    @TaskAction
    fun transformMetadata() {
        val transformation = GranularMetadataTransformation(
            params = transformationParameters,
            parentVisibleSourceSetsProvider = { parentVisibleSourceSetFiles.map(::readVisibleSourceSetsFile) }
        )

        if (outputsDir.isDirectory) {
            outputsDir.deleteRecursively()
        }
        outputsDir.mkdirs()

        val metadataDependencyResolutions = transformation.metadataDependencyResolutions

        metadataDependencyResolutions
            .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .forEach { chooseVisibleSourceSets ->
                objectFactory.transformMetadataLibrariesForBuild(chooseVisibleSourceSets, outputsDir, true)
            }

        writeTransformedLibraries(metadataDependencyResolutions)
        writeVisibleSourceSets(transformation.visibleSourceSetsByComponentId)
    }

    private fun writeTransformedLibraries(resolutions: Iterable<MetadataDependencyResolution>) {
        val fileList = resolutions.flatMap { resolution ->
            val files: Iterable<File> = when (resolution) {
                is MetadataDependencyResolution.ChooseVisibleSourceSets -> when (val metadataProvider = resolution.metadataProvider) {
                    is MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ArtifactMetadataProvider -> metadataProvider.read { content ->
                        resolution.allVisibleSourceSetNames.mapNotNull { sourceSet ->
                            val metadataBinary = content.getSourceSet(sourceSet).metadataBinary ?: return@mapNotNull null
                            val outputBinaryFile = outputsDir.resolve(metadataBinary.relativeFile)
                            metadataBinary.copyTo(outputBinaryFile)
                            outputBinaryFile
                        }
                    }
                    is MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider -> {
                        resolution.allVisibleSourceSetNames.flatMap { sourceSet ->
                            metadataProvider.getSourceSetCompiledMetadata(sourceSet).files
                        }
                    }
                }
                is MetadataDependencyResolution.Exclude -> emptySet()
                is MetadataDependencyResolution.KeepOriginalDependency -> transformationParameters
                    .resolvedMetadataConfiguration
                    .componentArtifacts(resolution.dependency)
                    .map { it.file }
            }

            files
        }

        val content = fileList.joinToString("\n")

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

    @get:Internal // Warning! transformedLibraries is available only after Task Execution
    val transformedLibraries: Provider<List<File>> get() = transformedLibrariesFileIndex.map { regularFile ->
        regularFile.asFile.readLines().map { File(it) }
    }
}
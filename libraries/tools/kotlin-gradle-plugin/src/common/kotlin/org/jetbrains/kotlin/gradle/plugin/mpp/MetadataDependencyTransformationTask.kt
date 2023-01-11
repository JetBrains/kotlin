/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ArtifactMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.*
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.metadata.ResolvedMetadataFilesProvider
import org.jetbrains.kotlin.gradle.targets.metadata.dependsOnClosureWithInterCompilationDependencies
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.mergeWith
import org.jetbrains.kotlin.gradle.utils.outputFilesProvider
import java.io.File
import javax.inject.Inject

/* Keep typealias for source compatibility */
@Suppress("unused")
@Deprecated("Task was renamed to MetadataDependencyTransformationTask", replaceWith = ReplaceWith("MetadataDependencyTransformationTask"))
typealias TransformKotlinGranularMetadata = MetadataDependencyTransformationTask

open class MetadataDependencyTransformationTask
@Inject constructor(
    kotlinSourceSet: KotlinSourceSet,
    private val objectFactory: ObjectFactory
) : DefaultTask() {

    private val params = GranularMetadataTransformation.Params(
        project,
        kotlinSourceSet,
    )

//    init {
//        notCompatibleWithConfigurationCacheCompat(
//            "Task $name does not support Gradle Configuration Cache. Check KT-49933 for more info"
//        )
//    }


    @get:OutputDirectory
    val outputsDir: File by lazy {
        project.kotlinTransformedMetadataLibraryDirectoryForBuild(params.sourceSetName)
    }

    @Suppress("unused") // Gradle input
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    internal val configurationToResolve: FileCollection = kotlinSourceSet.internal.resolvableMetadataConfiguration

//    private val participatingSourceSets: Set<KotlinSourceSet> get() {
//        return kotlinSourceSet.internal.withDependsOnClosure.toMutableSet().apply {
//            if (any { it.name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME })
//                add(project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME))
//        }
//    }
//
//    @Suppress("unused") // Gradle input
//    @get:Input
//    internal val inputSourceSetsAndCompilations: Map<String, Iterable<String>> by lazy {
//        participatingSourceSets.associate { sourceSet ->
//            sourceSet.name to sourceSet.internal.compilations.map { it.name }.sorted()
//        }
//    }
//
//    private val participatingCompilations: Iterable<KotlinCompilation<*>>
//        get() = participatingSourceSets.flatMap { it.internal.compilations }.toSet()
//
//    @Suppress("unused") // Gradle input
//    @get:Input
//    internal val inputCompilationDependencies: Map<String, Set<List<String?>>> by lazy {
//        participatingCompilations.associate {
//            it.name to project.configurations.getByName(it.compileDependencyConfigurationName)
//                .allDependencies.map { listOf(it.group, it.name, it.version) }.toSet()
//        }
//    }

    private fun writeVisibleSourceSets(map: Map<String, Set<String>>) {
        val content = map.entries.joinToString("\n") { (id, visibleSourceSets) ->
            "$id => ${visibleSourceSets.joinToString(",")}"
        }
        visibleSourceSetsFile.get().asFile.writeText(content)
    }

    private val transformation: GranularMetadataTransformation get() {
        return GranularMetadataTransformation(
            params,
            lazy {
                parentVisibleSourceSetFiles
                    .map { visibleSourceSets(it) }
                    .reduceOrNull { acc, map -> acc mergeWith map }
                    .orEmpty()
            }
        )
    }

    @get:Internal
    internal val metadataDependencyResolutions: Iterable<MetadataDependencyResolution> get() = transformation.metadataDependencyResolutions

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
                    objectFactory.transformMetadataLibrariesForBuild(
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

        val transformation = this.transformation

        transformation.metadataDependencyResolutions
            .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .forEach { chooseVisibleSourceSets ->
                objectFactory.transformMetadataLibrariesForBuild(chooseVisibleSourceSets, outputsDir, true)
            }

        val serializer = MetadataDependencyResolutionSerializer(transformation.params)
        metadataDependencyResolutionsFile.get().asFile.writeText(serializer.serializeList(metadataDependencyResolutions))

        val transformedLibraries = metadataDependencyResolutions.flatMap { resolution ->
            val files: Iterable<File> = when (resolution) {
                is MetadataDependencyResolution.ChooseVisibleSourceSets -> when (val metadataProvider = resolution.metadataProvider) {
                    is ArtifactMetadataProvider -> metadataProvider.read { content ->
                        resolution.allVisibleSourceSetNames.mapNotNull { sourceSet ->
                            val metadataBinary = content.getSourceSet(sourceSet).metadataBinary ?: return@mapNotNull null
                            val outputBinaryFile = outputsDir.resolve(metadataBinary.relativeFile)
                            metadataBinary.copyTo(outputBinaryFile)
                            outputBinaryFile
                        }
                    }
                    is ProjectMetadataProvider -> {
                        resolution.allVisibleSourceSetNames.flatMap { sourceSet ->
                            metadataProvider.getSourceSetCompiledMetadata(sourceSet).files
                        }
                    }
                }
                is MetadataDependencyResolution.Exclude -> emptySet()
                is MetadataDependencyResolution.KeepOriginalDependency -> transformation
                    .params
                    .resolvedMetadataConfiguration
                    .dependencyArtifacts(resolution.resolvedDependency)
                    .map { it.file }
            }

            files
        }
        transformedLibrariesFileIndex.get().asFile.writeText(transformedLibraries.joinToString("\n"))
        writeVisibleSourceSets(transformation.ownVisibleSourceSets)
    }

    @get:OutputFile
    val metadataDependencyResolutionsFile: RegularFileProperty = project
        .objects
        .fileProperty()
        .apply{ set(outputsDir.resolve("${kotlinSourceSet.name}.metadataDependencyResolutions")) }

    @get:OutputFile
    val transformedLibrariesFileIndex: RegularFileProperty = objectFactory
        .fileProperty()
        .apply { set(outputsDir.resolve("${kotlinSourceSet.name}.libraries")) }

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

    private fun visibleSourceSets(from: File) = from
        .readLines()
        .associate { string ->
                val (id, visibleSourceSetsString) = string.split(" => ")
                id to visibleSourceSetsString.split(",").toSet()
            }

    @get:Internal
    val transformedLibraries: Provider<List<File>> get() = transformedLibrariesFileIndex.map { regularFile ->
        regularFile.asFile.readLines().map { File(it) }
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

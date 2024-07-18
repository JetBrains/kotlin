/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.dependsOnClosureWithInterCompilationDependencies
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.*
import java.io.File
import javax.inject.Inject

/* Keep typealias for source compatibility */
@Suppress("unused")
@Deprecated("Task was renamed to MetadataDependencyTransformationTask", replaceWith = ReplaceWith("MetadataDependencyTransformationTask"))
typealias TransformKotlinGranularMetadata = MetadataDependencyTransformationTask

internal const val TRANSFORM_ALL_SOURCESETS_DEPENDENCIES_METADATA = "transformDependenciesMetadata"
internal fun transformGranularMetadataTaskName(sourceSetName: String) =
    lowerCamelCaseName("transform", sourceSetName, "DependenciesMetadata")

internal fun Project.locateOrRegisterMetadataDependencyTransformationTask(
    sourceSet: KotlinSourceSet
): TaskProvider<MetadataDependencyTransformationTask> {
    val transformationTask = project.locateOrRegisterTask<MetadataDependencyTransformationTask>(
        transformGranularMetadataTaskName(sourceSet.name),
        listOf(sourceSet)
    ) {
        description =
            "Generates serialized dependencies metadata for compilation '${sourceSet.name}' (for tooling)"
    }

    project.locateOrRegisterTask<Task>(TRANSFORM_ALL_SOURCESETS_DEPENDENCIES_METADATA).dependsOn(transformationTask)

    return transformationTask
}

@DisableCachingByDefault(because = "Metadata Dependency Transformation Task doesn't benefit from caching as it doesn't have heavy load")
open class MetadataDependencyTransformationTask
@Inject constructor(
    kotlinSourceSet: KotlinSourceSet,
    private val objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout
) : DefaultTask() {

    //region Task Configuration State & Inputs
    private val transformationParameters = GranularMetadataTransformation.Params(project, kotlinSourceSet)

    @Suppress("unused") // task inputs for up-to-date checks
    @get:Nested
    internal val taskInputs = MetadataDependencyTransformationTaskInputs(project, kotlinSourceSet)

    @get:OutputDirectory
    internal val outputsDir: File get() = projectLayout.kotlinTransformedMetadataLibraryDirectoryForBuild(transformationParameters.sourceSetName)

    @Transient // Only needed for configuring task inputs
    private val parentTransformationTasksLazy: Lazy<List<TaskProvider<MetadataDependencyTransformationTask>>>? = lazy {
        dependsOnClosureWithInterCompilationDependencies(kotlinSourceSet).mapNotNull {
            project
                .tasks
                .locateTask(transformGranularMetadataTaskName(it.name))
        }
    }

    private val parentTransformationTasks: List<TaskProvider<MetadataDependencyTransformationTask>>
        get() = parentTransformationTasksLazy?.value
            ?: error(
                "`parentTransformationTasks` is null. " +
                        "Probably it is accessed it during Task Execution with state loaded from Configuration Cache"
            )

    @get:OutputFile
    protected val transformedLibrariesIndexFile: RegularFileProperty = objectFactory
        .fileProperty()
        .apply { set(outputsDir.resolve("${kotlinSourceSet.name}.json")) }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val parentLibrariesIndexFiles: SetProperty<RegularFile> = objectFactory.setProperty<RegularFile>().apply {
        parentTransformationTasks.forEach { taskProvider ->
            add(taskProvider.flatMap { it.transformedLibrariesIndexFile })
        }
    }

    //endregion Task Configuration State & Inputs

    private fun Iterable<MetadataDependencyResolution>.toTransformedLibrariesRecords(): List<TransformedMetadataLibraryRecord> =
        flatMap { resolution ->
            when (resolution) {
                is MetadataDependencyResolution.ChooseVisibleSourceSets -> resolution.toTransformedLibrariesRecords()
                is MetadataDependencyResolution.KeepOriginalDependency -> resolution.toTransformedLibrariesRecords()
                is MetadataDependencyResolution.Exclude -> emptyList()
            }
        }

    private fun MetadataDependencyResolution.KeepOriginalDependency.toTransformedLibrariesRecords(): List<TransformedMetadataLibraryRecord> {
        return transformationParameters.resolvedMetadataConfiguration.getArtifacts(dependency).map {
            TransformedMetadataLibraryRecord(
                moduleId = dependency.id.serializableUniqueKey,
                file = it.file.absolutePath,
                sourceSetName = null
            )
        }
    }

    private fun MetadataDependencyResolution.ChooseVisibleSourceSets.toTransformedLibrariesRecords(): List<TransformedMetadataLibraryRecord> {
        val moduleId = dependency.id.serializableUniqueKey
        val transformedLibraries = transformMetadataLibrariesForBuild(this, outputsDir, true)
        return transformedLibraries.flatMap { (sourceSetName, libraryFiles) ->
            libraryFiles.map { file ->
                TransformedMetadataLibraryRecord(
                    moduleId = moduleId,
                    file = file.absolutePath,
                    sourceSetName = sourceSetName
                )
            }
        }
    }

    @TaskAction
    fun transformMetadata() {
        val parentLibrariesRecords: List<List<TransformedMetadataLibraryRecord>> = parentLibrariesIndexFiles
            .get()
            .map { librariesIndexFile -> librariesIndexFile.records() }

        val visibleParentSourceSetsByModuleId = parentLibrariesRecords
            .flatten()
            .groupBy(keySelector = { it.moduleId }, valueTransform = { it.sourceSetName })

        val transformation = GranularMetadataTransformation(
            params = transformationParameters,
            parentSourceSetVisibilityProvider = ParentSourceSetVisibilityProvider { identifier: ComponentIdentifier ->
                val serializableKey = identifier.serializableUniqueKey
                visibleParentSourceSetsByModuleId[serializableKey].orEmpty().filterNotNull().toSet()
            }
        )

        if (outputsDir.isDirectory) {
            outputsDir.deleteRecursively()
        }
        outputsDir.mkdirs()

        val metadataDependencyResolutions = transformation.metadataDependencyResolutions
        val transformedLibrariesRecords = metadataDependencyResolutions.toTransformedLibrariesRecords()
        KotlinMetadataLibrariesIndexFile(transformedLibrariesIndexFile.get().asFile).write(transformedLibrariesRecords)
    }

    internal fun allTransformedLibraries(): Provider<List<File>> {
        val ownRecords = transformedLibrariesIndexFile.map { it.records() }

        return parentLibrariesIndexFiles.zip(ownRecords) { parent, own ->
            val allRecords = own + parent.flatMap { it.records() }
            allRecords.distinctBy { it.moduleId to it.sourceSetName }.map { File(it.file) }
        }
    }

    companion object {
        @JvmStatic
        private fun RegularFile.records() = KotlinMetadataLibrariesIndexFile(asFile).read()
    }
}


private typealias SerializableComponentIdentifierKey = String


/**
 * This unique key can be used to lookup various info for related Resolved Dependency
 * that gets serialized
 */
private val ComponentIdentifier.serializableUniqueKey
    get(): SerializableComponentIdentifierKey = when (this) {
        is ProjectComponentIdentifier -> "project ${build.buildPathCompat}$projectPath"
        is ModuleComponentIdentifier -> "module $group:$module:$version"
        else -> error("Unexpected Component Identifier: '$this' of type ${this.javaClass}")
    }
/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.plugin.ide.ideaImportDependsOn
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ArtifactMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.contains
import org.jetbrains.kotlin.gradle.utils.currentBuild
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import java.io.File
import java.io.Serializable
import java.util.concurrent.Callable
import javax.inject.Inject

internal val KotlinSourceSet.cinteropMetadataDependencyTransformationTaskName: String
    get() = lowerCamelCaseName("transform", name, "CInteropDependenciesMetadata")

internal val KotlinSourceSet.cinteropMetadataDependencyTransformationForIdeTaskName: String
    get() = lowerCamelCaseName("transform", name, "CInteropDependenciesMetadataForIde")

internal suspend fun Project.locateOrRegisterCInteropMetadataDependencyTransformationTask(
    sourceSet: DefaultKotlinSourceSet,
): TaskProvider<CInteropMetadataDependencyTransformationTask>? {
    if (!cInteropCommonizationEnabled()) return null
    if (sourceSet.internal.commonizerTarget.await() !is SharedCommonizerTarget) return null

    return locateOrRegisterTask(
        sourceSet.cinteropMetadataDependencyTransformationTaskName,
        args = listOf(
            sourceSet,
            /* outputDirectory = */
            project.layout.kotlinTransformedCInteropMetadataLibraryDirectoryForBuild(sourceSet.name),
            /* cleaning = */
            CInteropMetadataDependencyTransformationTask.Cleaning.DeleteOutputDirectory,
        ),
        configureTask = { configureTaskOrder() }
    )
}

internal suspend fun Project.locateOrRegisterCInteropMetadataDependencyTransformationTaskForIde(
    sourceSet: DefaultKotlinSourceSet,
): TaskProvider<CInteropMetadataDependencyTransformationTask>? {
    if (!cInteropCommonizationEnabled()) return null
    if (sourceSet.internal.commonizerTarget.await() !is SharedCommonizerTarget) return null

    return locateOrRegisterTask(
        sourceSet.cinteropMetadataDependencyTransformationForIdeTaskName,
        invokeWhenRegistered = {
            @OptIn(Idea222Api::class)
            ideaImportDependsOn(this)

            /* Older IDEs will still enqueue 'runCommonizer' task before import */
            @Suppress("deprecation")
            runCommonizerTask.dependsOn(this)
        },
        args = listOf(
            sourceSet,
            /* outputDirectory = */
            project.kotlinTransformedCInteropMetadataLibraryDirectoryForIde,
            /* cleaning = */
            CInteropMetadataDependencyTransformationTask.Cleaning.None,
        ),
        configureTask = { configureTaskOrder() }
    )
}

/**
 * The transformation tasks will internally access the lazy [GranularMetadataTransformation.metadataDependencyResolutionsOrEmpty] property
 * which internally will potentially resolve dependencies. Having multiple tasks accessing this synchronized lazy property
 * during execution and/or configuration phase will result in an internal deadlock in Gradle
 * `DefaultResourceLockCoordinationService.withStateLock`
 *
 * To avoid this deadlock tasks shall be ordered, so that dependsOn source sets (and source sets visible based on associate compilations)
 * will run the transformation first.
 */
private fun CInteropMetadataDependencyTransformationTask.configureTaskOrder() {
    val tasksForVisibleSourceSets = Callable {
        val allVisibleSourceSets = sourceSet.dependsOnClosure + sourceSet.getAdditionalVisibleSourceSets()
        project.tasks.withType<CInteropMetadataDependencyTransformationTask>().matching { it.sourceSet in allVisibleSourceSets }
    }
    mustRunAfter(tasksForVisibleSourceSets)
}


@DisableCachingByDefault(because = "Metadata Dependency Transformation Task doesn't benefit from caching as it doesn't have heavy load")
internal open class CInteropMetadataDependencyTransformationTask @Inject constructor(
    @Transient @get:Internal val sourceSet: DefaultKotlinSourceSet,
    @get:OutputDirectory val outputDirectory: File,
    @get:Internal val cleaning: Cleaning,
    objectFactory: ObjectFactory,
) : DefaultTask() {

    private val currentBuild = project.currentBuild

    private val parameters = GranularMetadataTransformation.Params(project, sourceSet)

    sealed class Cleaning : Serializable {
        abstract fun cleanOutputDirectory(outputDirectory: File)

        object DeleteOutputDirectory : Cleaning() {
            override fun cleanOutputDirectory(outputDirectory: File) {
                if (outputDirectory.isDirectory) outputDirectory.deleteRecursively()
            }
        }

        object None : Cleaning() {
            override fun cleanOutputDirectory(outputDirectory: File) = Unit
        }
    }

    @get:Nested
    internal val inputs = MetadataDependencyTransformationTaskInputs(project, sourceSet, keepProjectDependencies = false)

    @get:OutputFile
    protected val outputLibrariesFileIndex: RegularFileProperty = objectFactory
        .fileProperty()
        .apply { set(outputDirectory.resolve("${project.path.replace(":", ".")}-${sourceSet.name}.cinteropLibraries")) }

    @get:Internal
    internal val outputLibraryFiles: FileCollection = project.filesProvider {
        outputLibrariesFileIndex.map { file ->
            KotlinMetadataLibrariesIndexFile(file.asFile).read()
        }
    }

    @TaskAction
    protected fun transformDependencies() {
        cleaning.cleanOutputDirectory(outputDirectory)
        outputDirectory.mkdirs()
        /* Warning:
        Passing an empty ParentSourceSetVisibilityProvider will create ChooseVisibleSourceSet instances
        with bad 'visibleSourceSetNamesExcludingDependsOn'. This is okay, since cinterop transformations do not look
        into this field
         */
        val transformation = GranularMetadataTransformation(parameters, ParentSourceSetVisibilityProvider.Empty)
        val chooseVisibleSourceSets = transformation.metadataDependencyResolutions.resolutionsToTransform()
        val transformedLibraries = chooseVisibleSourceSets.flatMap(::materializeMetadata)
        KotlinMetadataLibrariesIndexFile(outputLibrariesFileIndex.get().asFile).write(transformedLibraries)
    }

    private fun materializeMetadata(
        chooseVisibleSourceSets: ChooseVisibleSourceSets,
    ): Iterable<File> {
        return when (val metadataProvider = chooseVisibleSourceSets.metadataProvider) {
            /* Project to Project commonized cinterops are shared using configurations */
            is ProjectMetadataProvider -> emptyList()

            /* Extract/Materialize all cinterop files from composite jar file */
            is ArtifactMetadataProvider -> metadataProvider.read { artifactContent ->
                val visibleSourceSetName = chooseVisibleSourceSets.visibleSourceSetProvidingCInterops ?: return emptyList()
                val sourceSetContent = artifactContent.findSourceSet(visibleSourceSetName) ?: return emptyList()
                sourceSetContent.cinteropMetadataBinaries
                    .onEach { cInteropMetadataBinary -> cInteropMetadataBinary.copyIntoDirectory(outputDirectory) }
                    .map { cInteropMetadataBinary -> outputDirectory.resolve(cInteropMetadataBinary.relativeFile) }
            }
        }
    }

    private fun Iterable<MetadataDependencyResolution>.resolutionsToTransform(): List<ChooseVisibleSourceSets> {
        return filterIsInstance<ChooseVisibleSourceSets>()
            .filterNot { it.dependency in currentBuild }
    }
}


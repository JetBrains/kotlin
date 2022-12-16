/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.plugin.ide.ideaImportDependsOn
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ArtifactMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.toKpmModuleIdentifiers
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.compileDependenciesTransformationOrFail
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.notCompatibleWithConfigurationCacheCompat
import org.jetbrains.kotlin.gradle.utils.outputFilesProvider
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.project.model.KpmModuleIdentifier
import java.io.File
import java.io.Serializable
import java.util.concurrent.Callable
import javax.inject.Inject

internal val KotlinSourceSet.cinteropMetadataDependencyTransformationTaskName: String
    get() = lowerCamelCaseName("transform", name, "CInteropDependenciesMetadata")

internal val KotlinSourceSet.cinteropMetadataDependencyTransformationForIdeTaskName: String
    get() = lowerCamelCaseName("transform", name, "CInteropDependenciesMetadataForIde")

internal fun Project.locateOrRegisterCInteropMetadataDependencyTransformationTask(
    sourceSet: DefaultKotlinSourceSet,
): TaskProvider<CInteropMetadataDependencyTransformationTask>? {
    if (!kotlinPropertiesProvider.enableCInteropCommonization) return null

    return locateOrRegisterTask(
        sourceSet.cinteropMetadataDependencyTransformationTaskName,
        args = listOf(
            sourceSet,
            /* outputDirectory = */
            project.kotlinTransformedCInteropMetadataLibraryDirectoryForBuild(sourceSet.name),
            /* outputLibraryFilesDiscovery = */
            CInteropMetadataDependencyTransformationTask.OutputLibraryFilesDiscovery.ScanOutputDirectory,
            /* cleaning = */
            CInteropMetadataDependencyTransformationTask.Cleaning.DeleteOutputDirectory
        ),
        configureTask = { configureTaskOrder(); onlyIfSourceSetIsSharedNative() }
    )
}

internal fun Project.locateOrRegisterCInteropMetadataDependencyTransformationTaskForIde(
    sourceSet: DefaultKotlinSourceSet,
): TaskProvider<CInteropMetadataDependencyTransformationTask>? {
    if (!kotlinPropertiesProvider.enableCInteropCommonization) return null

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
            /* outputLibraryFilesDiscovery = */
            CInteropMetadataDependencyTransformationTask.OutputLibraryFilesDiscovery.Precise,
            /* cleaning = */
            CInteropMetadataDependencyTransformationTask.Cleaning.None
        ),
        configureTask = { configureTaskOrder(); onlyIfSourceSetIsSharedNative() }
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

private fun CInteropMetadataDependencyTransformationTask.onlyIfSourceSetIsSharedNative() {
    onlyIf { getCommonizerTarget(sourceSet) is SharedCommonizerTarget }
}

internal open class CInteropMetadataDependencyTransformationTask @Inject constructor(
    @Transient @get:Internal val sourceSet: DefaultKotlinSourceSet,
    @get:OutputDirectory val outputDirectory: File,
    @get:Internal val outputLibraryFilesDiscovery: OutputLibraryFilesDiscovery,
    @get:Internal val cleaning: Cleaning
) : DefaultTask() {

    init {
        notCompatibleWithConfigurationCacheCompat(
            "Task $name does not support Gradle Configuration Cache. Check KT-49933 for more info"
        )
    }

    sealed class OutputLibraryFilesDiscovery : Serializable {
        abstract fun resolveOutputLibraryFiles(outputDirectory: File, resolutions: Iterable<ChooseVisibleSourceSets>): Set<File>

        /**
         * Can be used when the used output directory is only used by this task.
         * In this case, all libraries in the given outputDirectory will be library files produced by this task, so just
         * scanning its content will be enough.
         */
        object ScanOutputDirectory : OutputLibraryFilesDiscovery() {
            override fun resolveOutputLibraryFiles(outputDirectory: File, resolutions: Iterable<ChooseVisibleSourceSets>): Set<File> {
                return outputDirectory.walkTopDown().maxDepth(2).filter { it.isFile && it.extension == KLIB_FILE_EXTENSION }.toSet()
            }
        }

        /**
         * Will actually read the [CompositeMetadataArtifact] and infer the exact file locations.
         * This can be used if the output directory might be shared with other tasks.
         */
        object Precise : OutputLibraryFilesDiscovery() {
            override fun resolveOutputLibraryFiles(outputDirectory: File, resolutions: Iterable<ChooseVisibleSourceSets>): Set<File> {
                return resolutions.flatMap { chooseVisibleSourceSets ->
                    /* This task only cares about extracting artifacts. Project to Project dependencies can return emptyList */
                    if (chooseVisibleSourceSets.metadataProvider !is ArtifactMetadataProvider) return@flatMap emptyList()
                    chooseVisibleSourceSets.metadataProvider.read { artifactContent ->
                        chooseVisibleSourceSets.visibleSourceSetsProvidingCInterops
                            .mapNotNull { visibleSourceSetName -> artifactContent.findSourceSet(visibleSourceSetName) }
                            .flatMap { sourceSetContent -> sourceSetContent.cinteropMetadataBinaries }
                            .map { cInteropMetadataBinary -> outputDirectory.resolve(cInteropMetadataBinary.relativeFile) }
                    }
                }.toSet()
            }
        }
    }

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

    @Suppress("unused")
    class ChooseVisibleSourceSetProjection(
        @Input val dependencyModuleIdentifiers: List<KpmModuleIdentifier>,
        @Nested val projectStructureMetadata: KotlinProjectStructureMetadata,
        @Input val visibleSourceSetsProvidingCInterops: Set<String>
    ) {
        constructor(chooseVisibleSourceSets: ChooseVisibleSourceSets) : this(
            dependencyModuleIdentifiers = chooseVisibleSourceSets.dependency.toKpmModuleIdentifiers(),
            projectStructureMetadata = chooseVisibleSourceSets.projectStructureMetadata,
            visibleSourceSetsProvidingCInterops = chooseVisibleSourceSets.visibleSourceSetsProvidingCInterops
        )
    }

    @Suppress("unused")
    @get:Classpath
    protected val inputArtifactFiles: FileCollection get() = sourceSet
        .compileDependenciesTransformationOrFail
        .configurationToResolve
        .withoutProjectDependencies()

    @get:Internal
    protected val chooseVisibleSourceSets
        get() = sourceSet
            .compileDependenciesTransformationOrFail
            .metadataDependencyResolutions
            .filterIsInstance<ChooseVisibleSourceSets>()

    @Suppress("unused")
    @get:Nested
    protected val chooseVisibleSourceSetsProjection
        get() = chooseVisibleSourceSets.map(::ChooseVisibleSourceSetProjection).toSet()

    @get:Internal
    val outputLibraryFiles = outputFilesProvider(lazy {
        outputLibraryFilesDiscovery.resolveOutputLibraryFiles(outputDirectory, chooseVisibleSourceSets)
    })

    @TaskAction
    protected fun transformDependencies() {
        cleaning.cleanOutputDirectory(outputDirectory)
        if (getCommonizerTarget(sourceSet) !is SharedCommonizerTarget) return
        chooseVisibleSourceSets.forEach(::materializeMetadata)
    }

    private fun materializeMetadata(
        chooseVisibleSourceSets: ChooseVisibleSourceSets
    ): Unit = when (chooseVisibleSourceSets.metadataProvider) {
        /* Nothing to transform: We will use original commonizer output in such cases */
        is ProjectMetadataProvider -> Unit

        /* Extract/Materialize all cinterop files from composite jar file */
        is ArtifactMetadataProvider -> chooseVisibleSourceSets.metadataProvider.read { artifactContent ->
            chooseVisibleSourceSets.visibleSourceSetsProvidingCInterops
                .mapNotNull { visibleSourceSetName -> artifactContent.findSourceSet(visibleSourceSetName) }
                .flatMap { sourceSetContent -> sourceSetContent.cinteropMetadataBinaries }
                .forEach { cInteropMetadataBinary -> cInteropMetadataBinary.copyIntoDirectory(outputDirectory) }
        }
    }

    private fun Configuration.withoutProjectDependencies(): FileCollection {
        return incoming.artifactView { view ->
            view.componentFilter { componentIdentifier ->
                componentIdentifier !is ProjectComponentIdentifier
            }
        }.files
    }
}

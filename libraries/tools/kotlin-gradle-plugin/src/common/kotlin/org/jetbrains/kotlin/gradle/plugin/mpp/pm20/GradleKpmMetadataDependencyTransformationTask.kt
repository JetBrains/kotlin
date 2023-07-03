/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.transformMetadataLibrariesForBuild
import org.jetbrains.kotlin.gradle.targets.metadata.ResolvedMetadataFilesProvider
import org.jetbrains.kotlin.gradle.utils.getValue
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
internal open class GradleKpmMetadataDependencyTransformationTask
@Inject constructor(
    @get:Internal
    @field:Transient
    val fragment: GradleKpmFragment,
    private val objectFactory: ObjectFactory,
    //FIXME annotations
    private val transformation: GradleKpmFragmentGranularMetadataResolver
) : DefaultTask() {

    @get:OutputDirectory
    val outputsDir: File by project.provider {
        project.buildDir.resolve("kotlinFragmentDependencyMetadata").resolve(fragment.disambiguateName(""))
    }

    @Suppress("unused") // Gradle input
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val allSourceSetsMetadataConfiguration: FileCollection by lazy {
        project.files(resolvableMetadataConfiguration(fragment.containingModule))
    }

    @Suppress("unused") // Gradle input
    @get:Input
    internal val inputFragmentsAndVariants: Map<String, Iterable<String>> by project.provider {
        val participatingFragments = fragment.withRefinesClosure
        participatingFragments.associateWith { it.containingVariants }
            .entries.associate { (fragment, variants) ->
                fragment.name to variants.map { it.fragmentName }.sorted()
            }
    }

    @Suppress("unused") // Gradle input
    @get:Input
    internal val inputVariantDependencies: Map<String, Set<List<String?>>> by project.provider {
        val participatingFragments = fragment.withRefinesClosure
        val participatingCompilations = participatingFragments.flatMap { it.containingVariants }
        participatingCompilations.associate { variant ->
            variant.fragmentName to variant.compileDependenciesConfiguration
                .allDependencies.map { listOf(it.group, it.name, it.version) }.toSet()
        }
    }

    @get:Internal
    @delegate:Transient // exclude from Gradle instant execution state
    internal val metadataDependencyResolutions: Iterable<MetadataDependencyResolution> by project.provider {
        transformation.resolutions
    }

    @get:Internal
    internal val filesByResolution: Map<out MetadataDependencyResolution, FileCollection>
        get() = metadataDependencyResolutions
            .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .associateWith { chooseVisibleSourceSets ->
                project.files(objectFactory.transformMetadataLibrariesForBuild(chooseVisibleSourceSets, outputsDir, materializeFiles = false))
                    .builtBy(this)
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
                objectFactory.transformMetadataLibrariesForBuild(chooseVisibleSourceSets, outputsDir, materializeFiles = true)
            }
    }
}

internal class FragmentResolvedMetadataProvider(
    taskProvider: TaskProvider<out GradleKpmMetadataDependencyTransformationTask>
) : ResolvedMetadataFilesProvider {
    override val buildDependencies: Iterable<TaskProvider<*>> = listOf(taskProvider)
    override val metadataResolutions: Iterable<MetadataDependencyResolution> by taskProvider.map { it.metadataDependencyResolutions }
    override val metadataFilesByResolution: Map<out MetadataDependencyResolution, FileCollection> by taskProvider.map { it.filesByResolution }
}

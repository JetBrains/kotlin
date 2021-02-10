/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationOutput
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinCompilationOutput
import org.jetbrains.kotlin.gradle.plugin.mpp.filterModuleName
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.targets.metadata.ResolvedMetadataFilesProvider
import org.jetbrains.kotlin.gradle.targets.metadata.createTransformedMetadataClasspath
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal class KotlinFragmentMetadataCompilationData(
    override val project: Project,
    val fragment: KotlinGradleFragment,
    private val module: KotlinGradleModule,
    private val compileAllTask: TaskProvider<DefaultTask>,
    val metadataCompilationRegistry: MetadataCompilationRegistry,
    private val resolvedMetadataFiles: Lazy<Iterable<ResolvedMetadataFilesProvider>>
) : KotlinCompilationData<KotlinMultiplatformCommonOptions> {

    override val owner
        get() = project.pm20Extension

    override val compilationPurpose: String
        get() = fragment.fragmentName

    override val compilationClassifier: String
        get() = lowerCamelCaseName(module.name, "metadata")

    override val kotlinSourceDirectoriesByFragmentName: Map<String, SourceDirectorySet>
        get() = mapOf(fragment.fragmentName to fragment.kotlinSourceRoots)

    override val compileKotlinTaskName: String
        get() = lowerCamelCaseName("compile", fragment.disambiguateName(""), "KotlinMetadata")

    override val compileAllTaskName: String
        get() = compileAllTask.name

    override val compileDependencyFiles: FileCollection by project.provider {
        createTransformedMetadataClasspath(
            project,
            resolvableMetadataConfiguration(fragment.containingModule),
            lazy { fragment.refinesClosure.minus(fragment).map { metadataCompilationRegistry.byFragment(it).output.classesDirs } },
            resolvedMetadataFiles
        )
    }

    override val output: KotlinCompilationOutput = DefaultKotlinCompilationOutput(
        project,
        project.provider { project.buildDir.resolve("processedResources/${fragment.disambiguateName("metadata")}") }
    )

    override val languageSettings: LanguageSettingsBuilder = DefaultLanguageSettingsBuilder() // FIXME apply settings

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.common

    override val moduleName: String
        get() { // FIXME deduplicate with ownModuleName
            val baseName = project.convention.findPlugin(BasePluginConvention::class.java)?.archivesBaseName
                ?: project.name
            val suffix = if (module.moduleClassifier == null) "" else "_${module.moduleClassifier}"
            return filterModuleName("$baseName$suffix")
        }

    override val ownModuleName: String
        get() = moduleName

    override val kotlinOptions: KotlinMultiplatformCommonOptions = // FIXME expose for configuration
        KotlinMultiplatformCommonOptionsImpl()

    override val friendPaths: Iterable<FileCollection>
        get() = metadataCompilationRegistry.run {
            fragment.refinesClosure.minus(fragment)
                .map {
                    metadataCompilationRegistry.byFragment(it).output.classesDirs
                }
        }
}

internal class MetadataCompilationRegistry {
    private val compilationDataPerFragment = mutableMapOf<KotlinGradleFragment, KotlinFragmentMetadataCompilationData>()

    fun register(fragment: KotlinGradleFragment, compilationData: KotlinFragmentMetadataCompilationData) {
        compilationDataPerFragment[fragment]?.let { error("compilation data for fragment $fragment already registered") }
        compilationDataPerFragment[fragment] = compilationData
        withAllCallbacks.forEach { it.invoke(compilationData) }
    }

    fun byFragment(fragment: KotlinGradleFragment): KotlinFragmentMetadataCompilationData =
        compilationDataPerFragment.getValue(fragment)

    private val withAllCallbacks = mutableListOf<(KotlinFragmentMetadataCompilationData) -> Unit>()

    fun withAll(action: (KotlinFragmentMetadataCompilationData) -> Unit) {
        compilationDataPerFragment.forEach { action(it.value) }
        withAllCallbacks += action
    }
}
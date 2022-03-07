/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptionsImpl
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.targets.metadata.ResolvedMetadataFilesProvider
import org.jetbrains.kotlin.gradle.targets.metadata.createMetadataDependencyTransformationClasspath
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.newProperty
import org.jetbrains.kotlin.gradle.utils.setValue
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

interface KotlinMetadataCompilationData<T : KotlinCommonOptions> : KotlinCompilationData<T> {
    val isActive: Boolean
}

interface KotlinCommonFragmentMetadataCompilationData : KotlinMetadataCompilationData<KotlinMultiplatformCommonOptions>

internal abstract class AbstractKotlinFragmentMetadataCompilationData<T : KotlinCommonOptions>(
    final override val project: Project,
    val fragment: KotlinGradleFragment,
    private val module: KotlinGradleModule,
    private val compileAllTask: TaskProvider<DefaultTask>,
    val metadataCompilationRegistry: MetadataCompilationRegistry,
    private val resolvedMetadataFiles: Lazy<Iterable<ResolvedMetadataFilesProvider>>
) : KotlinMetadataCompilationData<T> {

    override val owner
        get() = project.topLevelExtension

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

    override var compileDependencyFiles: FileCollection by project.newProperty {
        createMetadataDependencyTransformationClasspath(
            project = project,
            fromFiles = resolvableMetadataConfiguration(fragment.containingModule),
            parentCompiledMetadataFiles = lazy {
                fragment.refinesClosure.minus(fragment).map {
                    val compilation = metadataCompilationRegistry.getForFragmentOrNull(it)
                        ?: return@map project.files()
                    compilation.output.classesDirs
                }
            },
            metadataResolutionProviders = resolvedMetadataFiles
        )
    }

    override val output: KotlinCompilationOutput = DefaultKotlinCompilationOutput(
        project,
        project.provider { project.buildDir.resolve("processedResources/${fragment.disambiguateName("metadata")}") }
    )

    override val languageSettings: LanguageSettingsBuilder = fragment.languageSettings

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.common

    override val moduleName: String
        get() { // FIXME deduplicate with ownModuleName
            val baseName = project.archivesName
                ?: project.name
            val suffix = if (module.moduleClassifier == null) "" else "_${module.moduleClassifier}"
            return filterModuleName("$baseName$suffix")
        }

    override val ownModuleName: String
        get() = moduleName

    override val friendPaths: Iterable<FileCollection>
        get() = metadataCompilationRegistry.run {
            fragment.refinesClosure.minus(fragment)
                .map {
                    val compilation = metadataCompilationRegistry.getForFragmentOrNull(it)
                        ?: return@map project.files()
                    compilation.output.classesDirs
                }
        }
}

internal open class KotlinCommonFragmentMetadataCompilationDataImpl(
    project: Project,
    fragment: KotlinGradleFragment,
    module: KotlinGradleModule,
    compileAllTask: TaskProvider<DefaultTask>,
    metadataCompilationRegistry: MetadataCompilationRegistry,
    resolvedMetadataFiles: Lazy<Iterable<ResolvedMetadataFilesProvider>>
) : AbstractKotlinFragmentMetadataCompilationData<KotlinMultiplatformCommonOptions>(
    project,
    fragment,
    module,
    compileAllTask,
    metadataCompilationRegistry,
    resolvedMetadataFiles), KotlinCommonFragmentMetadataCompilationData {

    override val isActive: Boolean
        get() = !fragment.isNativeShared() &&
                fragment.containingModule.variantsContainingFragment(fragment).run {
                    !all { it.platformType in setOf(KotlinPlatformType.androidJvm, KotlinPlatformType.jvm) } &&
                            mapTo(hashSetOf()) { it.platformType }.size > 1
                }

    override val kotlinOptions: KotlinMultiplatformCommonOptions = KotlinMultiplatformCommonOptionsImpl()
}

interface KotlinNativeFragmentMetadataCompilationData :
    KotlinMetadataCompilationData<KotlinCommonOptions>,
    KotlinNativeCompilationData<KotlinCommonOptions>

internal fun KotlinGradleFragment.isNativeShared(): Boolean =
    containingModule.variantsContainingFragment(this).run {
        any() && all { it.platformType == KotlinPlatformType.native }
    }

internal fun KotlinGradleFragment.isNativeHostSpecific(): Boolean =
    this in getHostSpecificFragments(containingModule)

internal open class KotlinNativeFragmentMetadataCompilationDataImpl(
    project: Project,
    fragment: KotlinGradleFragment,
    module: KotlinGradleModule,
    compileAllTask: TaskProvider<DefaultTask>,
    metadataCompilationRegistry: MetadataCompilationRegistry,
    resolvedMetadataFiles: Lazy<Iterable<ResolvedMetadataFilesProvider>>
) : AbstractKotlinFragmentMetadataCompilationData<KotlinCommonOptions>(
    project,
    fragment,
    module,
    compileAllTask,
    metadataCompilationRegistry,
    resolvedMetadataFiles
), KotlinNativeFragmentMetadataCompilationData {

    override val compileKotlinTaskName: String
        get() = lowerCamelCaseName("compile", fragment.disambiguateName(""), "KotlinNativeMetadata")

    override val isActive: Boolean
        get() = fragment.isNativeShared() && fragment.containingModule.variantsContainingFragment(fragment).count() > 1

    override val kotlinOptions: NativeCompileOptions = NativeCompileOptions { languageSettings }

    override val konanTarget: KonanTarget
        get() {
            val nativeVariants =
                fragment.containingModule.variantsContainingFragment(fragment).filterIsInstance<KotlinNativeVariantInternal>()
            return nativeVariants.firstOrNull { it.konanTarget.enabledOnCurrentHost }?.konanTarget
                ?: nativeVariants.firstOrNull()?.konanTarget
                ?: HostManager.host
        }

    // FIXME endorsed libs?
    override val enableEndorsedLibs: Boolean
        get() = false
}

// TODO think about more generic case: a fragment that can be compiled by an arbitrary compiler
//      what tasks should we create? should there be a generic task for that?
internal class MetadataCompilationRegistry {
    private val commonCompilationDataPerFragment = mutableMapOf<KotlinGradleFragment, KotlinCommonFragmentMetadataCompilationDataImpl>()
    private val nativeCompilationDataPerFragment = mutableMapOf<KotlinGradleFragment, KotlinNativeFragmentMetadataCompilationDataImpl>()

    fun registerCommon(fragment: KotlinGradleFragment, compilationData: KotlinCommonFragmentMetadataCompilationDataImpl) {
        commonCompilationDataPerFragment.compute(fragment) { _, existing ->
            existing?.let { error("common compilation data for fragment $fragment already registered") }
            compilationData
        }
        withAllCommonCallbacks.forEach { it.invoke(compilationData) }
    }

    fun registerNative(fragment: KotlinGradleFragment, compilationData: KotlinNativeFragmentMetadataCompilationDataImpl) {
        nativeCompilationDataPerFragment.compute(fragment) { _, existing ->
            existing?.let { error("native compilation data for fragment $fragment already registered") }
            compilationData
        }
        withAllNativeCallbacks.forEach { it.invoke(compilationData) }
    }

    fun getForFragmentOrNull(fragment: KotlinGradleFragment): AbstractKotlinFragmentMetadataCompilationData<*>? =
        listOf(commonCompilationDataPerFragment.getValue(fragment), nativeCompilationDataPerFragment.getValue(fragment)).singleOrNull {
            it.isActive
        }

    private val withAllCommonCallbacks = mutableListOf<(AbstractKotlinFragmentMetadataCompilationData<*>) -> Unit>()
    private val withAllNativeCallbacks = mutableListOf<(AbstractKotlinFragmentMetadataCompilationData<*>) -> Unit>()

    fun withAll(action: (AbstractKotlinFragmentMetadataCompilationData<*>) -> Unit) {
        commonCompilationDataPerFragment.forEach { action(it.value) }
        nativeCompilationDataPerFragment.forEach { action(it.value) }
        withAllCommonCallbacks += action
        withAllNativeCallbacks += action
    }
}

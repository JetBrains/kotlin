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
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.targets.metadata.ResolvedMetadataFilesProvider
import org.jetbrains.kotlin.gradle.targets.native.NativeCompilerOptions
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.newProperty
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

interface GradleKpmMetadataCompilationData<T : KotlinCommonOptions> : GradleKpmCompilationData<T> {
    val isActive: Boolean
}

interface GradleKpmCommonFragmentMetadataCompilationData : GradleKpmMetadataCompilationData<KotlinMultiplatformCommonOptions>

internal abstract class GradleKpmAbstractFragmentMetadataCompilationData<T : KotlinCommonOptions>(
    final override val project: Project,
    val fragment: GradleKpmFragment,
    private val module: GradleKpmModule,
    private val compileAllTask: TaskProvider<DefaultTask>,
    val metadataCompilationRegistry: MetadataCompilationRegistry,
    private val resolvedMetadataFiles: Lazy<Iterable<ResolvedMetadataFilesProvider>>
) : GradleKpmMetadataCompilationData<T> {

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
        TODO("""Provide Compile Dependency Files.
            | See KotlinMetadataTargetConfigurator::configureMetadataDependenciesForCompilation for reference""".trimMargin())
    }

    override val output: KotlinCompilationOutput = DefaultKotlinCompilationOutput(
        project,
        project.provider { project.buildDir.resolve("processedResources/${fragment.disambiguateName("metadata")}") }
    )

    final override val languageSettings: LanguageSettingsBuilder = fragment.languageSettings

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.common

    override val moduleName: String
        get() { // FIXME deduplicate with ownModuleName
            val baseName = project.archivesName.orNull
                ?: project.name
            val suffix = if (module.moduleClassifier == null) "" else "_${module.moduleClassifier}"
            return filterModuleName("$baseName$suffix")
        }


    override val friendPaths: Iterable<FileCollection>
        get() = metadataCompilationRegistry.run {
            fragment.refinesClosure
                .map {
                    val compilation = metadataCompilationRegistry.getForFragmentOrNull(it)
                        ?: return@map project.files()
                    compilation.output.classesDirs
                }
        }
}

internal open class GradleKpmCommonFragmentMetadataCompilationDataImpl(
    project: Project,
    fragment: GradleKpmFragment,
    module: GradleKpmModule,
    compileAllTask: TaskProvider<DefaultTask>,
    metadataCompilationRegistry: MetadataCompilationRegistry,
    resolvedMetadataFiles: Lazy<Iterable<ResolvedMetadataFilesProvider>>
) : GradleKpmAbstractFragmentMetadataCompilationData<KotlinMultiplatformCommonOptions>(
    project,
    fragment,
    module,
    compileAllTask,
    metadataCompilationRegistry,
    resolvedMetadataFiles
), GradleKpmCommonFragmentMetadataCompilationData {

    override val isActive: Boolean
        get() = !fragment.isNativeShared() &&
                fragment.containingVariants.run {
                    !all { it.platformType in setOf(KotlinPlatformType.androidJvm, KotlinPlatformType.jvm) } &&
                            mapTo(hashSetOf()) { it.platformType }.size > 1
                }

    override val compilerOptions: HasCompilerOptions<KotlinMultiplatformCommonCompilerOptions> =
        object : HasCompilerOptions<KotlinMultiplatformCommonCompilerOptions> {
            override val options: KotlinMultiplatformCommonCompilerOptions =
                project.objects.newInstance(KotlinMultiplatformCommonCompilerOptionsDefault::class.java)
        }


    @Suppress("DEPRECATION")
    @Deprecated("Replaced with compilerOptions.options", replaceWith = ReplaceWith("compilerOptions.options"))
    override val kotlinOptions: KotlinMultiplatformCommonOptions = object : KotlinMultiplatformCommonOptions {
        override val options: KotlinMultiplatformCommonCompilerOptions
            get() = compilerOptions.options
    }
}

interface GradleKpmNativeFragmentMetadataCompilationData :
    GradleKpmMetadataCompilationData<KotlinCommonOptions>,
    GradleKpmNativeCompilationData<KotlinCommonOptions>

internal fun GradleKpmFragment.isNativeShared(): Boolean =
    containingVariants.run {
        any() && all { it.platformType == KotlinPlatformType.native }
    }

internal fun GradleKpmFragment.isNativeHostSpecific(): Boolean =
    this.project.future { this@isNativeHostSpecific in getHostSpecificFragments(containingModule) }.lenient.getOrNull() ?: false

internal open class GradleKpmNativeFragmentMetadataCompilationDataImpl(
    project: Project,
    fragment: GradleKpmFragment,
    module: GradleKpmModule,
    compileAllTask: TaskProvider<DefaultTask>,
    metadataCompilationRegistry: MetadataCompilationRegistry,
    resolvedMetadataFiles: Lazy<Iterable<ResolvedMetadataFilesProvider>>
) : GradleKpmAbstractFragmentMetadataCompilationData<KotlinCommonOptions>(
    project,
    fragment,
    module,
    compileAllTask,
    metadataCompilationRegistry,
    resolvedMetadataFiles
), GradleKpmNativeFragmentMetadataCompilationData {

    override val compileKotlinTaskName: String
        get() = lowerCamelCaseName("compile", fragment.disambiguateName(""), "KotlinNativeMetadata")

    override val isActive: Boolean
        get() = fragment.isNativeShared() && fragment.containingVariants.count() > 1

    override val compilerOptions: HasCompilerOptions<KotlinCommonCompilerOptions> = NativeCompilerOptions(project)

    @Suppress("DEPRECATION")
    @Deprecated("Replaced with compilerOptions.options", replaceWith = ReplaceWith("compilerOptions.options"))
    override val kotlinOptions: KotlinCommonOptions = object : KotlinCommonOptions {
        override val options: KotlinCommonCompilerOptions
            get() = compilerOptions.options
    }

    override val konanTarget: KonanTarget
        get() {
            val nativeVariants =
                fragment.containingVariants.filterIsInstance<GradleKpmNativeVariantInternal>()
            return nativeVariants.firstOrNull { it.konanTarget.enabledOnCurrentHost }?.konanTarget
                ?: nativeVariants.firstOrNull()?.konanTarget
                ?: HostManager.host
        }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated(
        "Please declare explicit dependency on kotlinx-cli. This option has no longer effect since 1.9.0",
        level = DeprecationLevel.ERROR
    )
    override val enableEndorsedLibs: Boolean
        get() = false
}

// TODO think about more generic case: a fragment that can be compiled by an arbitrary compiler
//      what tasks should we create? should there be a generic task for that?
internal class MetadataCompilationRegistry {
    private val commonCompilationDataPerFragment = LinkedHashMap<GradleKpmFragment, GradleKpmCommonFragmentMetadataCompilationDataImpl>()
    private val nativeCompilationDataPerFragment = LinkedHashMap<GradleKpmFragment, GradleKpmNativeFragmentMetadataCompilationDataImpl>()

    fun registerCommon(fragment: GradleKpmFragment, compilationData: GradleKpmCommonFragmentMetadataCompilationDataImpl) {
        commonCompilationDataPerFragment.compute(fragment) { _, existing ->
            existing?.let { error("common compilation data for fragment $fragment already registered") }
            compilationData
        }
        withAllCommonCallbacks.forEach { it.invoke(compilationData) }
    }

    fun registerNative(fragment: GradleKpmFragment, compilationData: GradleKpmNativeFragmentMetadataCompilationDataImpl) {
        nativeCompilationDataPerFragment.compute(fragment) { _, existing ->
            existing?.let { error("native compilation data for fragment $fragment already registered") }
            compilationData
        }
        withAllNativeCallbacks.forEach { it.invoke(compilationData) }
    }

    fun getForFragmentOrNull(fragment: GradleKpmFragment): GradleKpmAbstractFragmentMetadataCompilationData<*>? =
        listOf(commonCompilationDataPerFragment.getValue(fragment), nativeCompilationDataPerFragment.getValue(fragment)).singleOrNull {
            it.isActive
        }

    private val withAllCommonCallbacks = mutableListOf<(GradleKpmAbstractFragmentMetadataCompilationData<*>) -> Unit>()
    private val withAllNativeCallbacks = mutableListOf<(GradleKpmAbstractFragmentMetadataCompilationData<*>) -> Unit>()

    fun withAll(action: (GradleKpmAbstractFragmentMetadataCompilationData<*>) -> Unit) {
        commonCompilationDataPerFragment.forEach { action(it.value) }
        nativeCompilationDataPerFragment.forEach { action(it.value) }
        withAllCommonCallbacks += action
        withAllNativeCallbacks += action
    }
}

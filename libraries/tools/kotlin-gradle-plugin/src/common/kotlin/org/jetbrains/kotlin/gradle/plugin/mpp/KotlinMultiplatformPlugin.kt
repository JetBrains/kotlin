/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript
import org.jetbrains.kotlin.gradle.plugin.hierarchy.orNull
import org.jetbrains.kotlin.gradle.plugin.hierarchy.setupDefaultKotlinHierarchy
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.ide.locateOrRegisterIdeResolveDependenciesTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin.Companion.sourceSetFreeCompilerArgsPropertyName
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.addBuildListenerForXcode
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.runDeprecationDiagnostics
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.sources.awaitPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWasmTargetPreset
import org.jetbrains.kotlin.gradle.targets.native.createFatFrameworks
import org.jetbrains.kotlin.gradle.targets.native.internal.setupCInteropCommonizedCInteropApiElementsConfigurations
import org.jetbrains.kotlin.gradle.targets.native.tasks.artifact.registerKotlinArtifactsExtension
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.utils.checkGradleCompatibility
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.statistics.metrics.StringMetrics

class KotlinMultiplatformPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkGradleCompatibility("the Kotlin Multiplatform plugin")
        runDeprecationDiagnostics(project)

        project.plugins.apply(JavaBasePlugin::class.java)

        val kotlinMultiplatformExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

        setupDefaultPresets(project)
        customizeKotlinDependencies(project)
        configureSourceSets(project)
        setupTargetsBuildStatsReport(project)

        // set up metadata publishing
        kotlinMultiplatformExtension.targetFromPresetInternal(
            KotlinMetadataTargetPreset(project),
            METADATA_TARGET_NAME
        )
        project.registerKotlinArtifactsExtension()

        configurePublishingWithMavenPublish(project)

        kotlinMultiplatformExtension.targets.withType(InternalKotlinTarget::class.java).all { applyUserDefinedAttributes(it) }

        // propagate compiler plugin options to the source set language settings
        setupAdditionalCompilerArguments(project)
        project.setupGeneralKotlinExtensionParameters()

        project.pluginManager.apply(ScriptingGradleSubplugin::class.java)

        exportProjectStructureMetadataForOtherBuilds(kotlinMultiplatformExtension)

        // Ensure that the instance is created and configured during apply
        project.kotlinIdeMultiplatformImport
        project.locateOrRegisterIdeResolveDependenciesTask()
        project.launch { project.setupCInteropCommonizedCInteropApiElementsConfigurations() }
        project.addBuildListenerForXcode()
        project.whenEvaluated { kotlinMultiplatformExtension.createFatFrameworks() }
    }

    private fun exportProjectStructureMetadataForOtherBuilds(
        extension: KotlinMultiplatformExtension,
    ) {
        // Run in AfterEvaluate stage to avoid issues with Precompiled Script Plugins
        // When Gradle runs `:generatePrecompiledScriptPluginAccessors` it creates dummy project and
        // applies plugins from *.gradle.kts file to and generates accessors from it.
        // These dummy projects never gets evaluated and should not expose any Project Structure Metadata.
        // Putting registerProjectStructureMetadata in AfterEvaluate stage prevents PSM registration in dummy projects.
        extension.project.launchInStage(AfterEvaluateBuildscript) {
            GlobalProjectStructureMetadataStorage.registerProjectStructureMetadata(extension.project) {
                extension.kotlinProjectStructureMetadata
            }
        }
    }

    private fun setupAdditionalCompilerArguments(project: Project) {
        // common source sets use the compiler options from the metadata compilation:
        val metadataCompilation =
            project.multiplatformExtension.metadata().compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        val primaryCompilationsBySourceSet by lazy { // don't evaluate eagerly: Android targets are not created at this point
            val allCompilationsForSourceSets = project.multiplatformExtension.sourceSets.associateWith { sourceSet ->
                sourceSet.internal.compilations.filter { compilation -> compilation.target.platformType != KotlinPlatformType.common }
            }

            allCompilationsForSourceSets.mapValues { (_, compilations) -> // choose one primary compilation
                when (compilations.size) {
                    0 -> metadataCompilation
                    1 -> compilations.single()
                    else -> {
                        val sourceSetTargets = compilations.map { it.target }.distinct()
                        when (sourceSetTargets.size) {
                            1 -> sourceSetTargets.single().compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                                ?: // use any of the compilations for now, looks OK for Android TODO maybe reconsider
                                compilations.first()

                            else -> metadataCompilation
                        }
                    }
                }
            }
        }

        project.kotlinExtension.sourceSets.all { sourceSet ->
            (sourceSet.languageSettings as? DefaultLanguageSettingsBuilder)?.run {
                compilerPluginOptionsTask = lazy {
                    val associatedCompilation = primaryCompilationsBySourceSet[sourceSet] ?: metadataCompilation
                    project.tasks.getByName(associatedCompilation.compileKotlinTaskName) as AbstractKotlinCompileTool<*>
                }
            }
        }
    }

    fun setupDefaultPresets(project: Project) {
        @Suppress("DEPRECATION")
        with(project.multiplatformExtension.presets) {
            add(KotlinJvmTargetPreset(project))
            add(KotlinJsTargetPreset(project).apply { irPreset = null })
            add(KotlinJsIrTargetPreset(project).apply { mixedMode = false })
            add(
                KotlinJsTargetPreset(project).apply {
                    irPreset = KotlinJsIrTargetPreset(project).apply { mixedMode = true }
                }
            )
            add(KotlinWasmTargetPreset(project, KotlinWasmTargetType.JS))
            add(KotlinWasmTargetPreset(project, KotlinWasmTargetType.WASI))
            add(project.objects.newInstance(KotlinAndroidTargetPreset::class.java, project))
            add(KotlinJvmWithJavaTargetPreset(project))

            // Note: modifying these sets should also be reflected in the DSL code generator, see 'presetEntries.kt'
            val nativeTargetsWithHostTests = setOf(LINUX_X64, MACOS_X64, MACOS_ARM64, MINGW_X64)
            val nativeTargetsWithSimulatorTests =
                setOf(IOS_X64, IOS_SIMULATOR_ARM64, WATCHOS_X86, WATCHOS_X64, WATCHOS_SIMULATOR_ARM64, TVOS_X64, TVOS_SIMULATOR_ARM64)

            HostManager().targets
                .forEach { (_, konanTarget) ->
                    val targetToAdd = when (konanTarget) {
                        in nativeTargetsWithHostTests ->
                            KotlinNativeTargetWithHostTestsPreset(konanTarget.presetName, project, konanTarget)

                        in nativeTargetsWithSimulatorTests ->
                            KotlinNativeTargetWithSimulatorTestsPreset(konanTarget.presetName, project, konanTarget)

                        else -> KotlinNativeTargetPreset(konanTarget.presetName, project, konanTarget)
                    }

                    add(targetToAdd)
                }
        }
    }


    private fun configureSourceSets(project: Project) = with(project.multiplatformExtension) {
        /* Create 'commonMain' and 'commonTest' SourceSets */
        sourceSets.create(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)
        sourceSets.create(KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME)

        project.launch {
            project.setupDefaultKotlinHierarchy()
        }
    }

    private fun setupTargetsBuildStatsReport(project: Project) {
        project.multiplatformExtension.targets.all { target ->
            /* Report the platform to tbe build stats service */
            val targetName = if (target is KotlinNativeTarget)
                target.konanTarget.name
            else
                target.platformType.name
            KotlinBuildStatsService.getInstance()?.report(StringMetrics.MPP_PLATFORMS, targetName)
        }
    }


    companion object {
        const val METADATA_TARGET_NAME = "metadata"

        internal fun sourceSetFreeCompilerArgsPropertyName(sourceSetName: String) =
            "kotlin.mpp.freeCompilerArgsForSourceSet.$sourceSetName"
    }
}

internal fun Project.setupGeneralKotlinExtensionParameters() {
    project.launch {
        for (sourceSet in kotlinExtension.awaitSourceSets()) {
            val languageSettings = sourceSet.languageSettings
            if (languageSettings !is DefaultLanguageSettingsBuilder) continue

            val isMainSourceSet = sourceSet
                .internal
                .awaitPlatformCompilations()
                .any { KotlinSourceSetTree.orNull(it) == KotlinSourceSetTree.main }

            if (isMainSourceSet) {
                languageSettings.explicitApi = project.providers.provider {
                    project.kotlinExtension.explicitApiModeAsCompilerArg()
                }
            }

            languageSettings.freeCompilerArgsProvider = project.provider {
                val propertyValue = with(project.extensions.extraProperties) {
                    val sourceSetFreeCompilerArgsPropertyName = sourceSetFreeCompilerArgsPropertyName(sourceSet.name)
                    if (has(sourceSetFreeCompilerArgsPropertyName)) {
                        get(sourceSetFreeCompilerArgsPropertyName)
                    } else null
                }

                mutableListOf<String>().apply {
                    when (propertyValue) {
                        is String -> add(propertyValue)
                        is Iterable<*> -> addAll(propertyValue.map { it.toString() })
                    }
                }
            }
        }
    }
}

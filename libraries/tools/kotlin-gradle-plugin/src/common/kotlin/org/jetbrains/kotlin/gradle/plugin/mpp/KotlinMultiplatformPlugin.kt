/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.supportedTargets
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
import org.jetbrains.kotlin.gradle.targets.native.*
import org.jetbrains.kotlin.gradle.targets.native.createFatFrameworks
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerTask
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropIdentifier
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizeCInteropTask
import org.jetbrains.kotlin.gradle.targets.native.internal.setupCInteropCommonizedCInteropApiElementsConfigurations
import org.jetbrains.kotlin.gradle.targets.native.tasks.artifact.registerKotlinArtifactsExtension
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.checkGradleCompatibility
import org.jetbrains.kotlin.gradle.utils.mapToFile
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.runCommand
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.nio.file.Paths

class KotlinMultiplatformPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkGradleCompatibility("the Kotlin Multiplatform plugin")
        runDeprecationDiagnostics(project)

        project.plugins.apply(JavaBasePlugin::class.java)

        val kotlinMultiplatformExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        project.addExtension(foreignDependenciesContainerExtension, project.objects.newInstance<ForeignDependencyContainer>())

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

        project.launch {
            KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript.await()
            processForeignDependencies(project)
        }
    }

    private fun exportProjectStructureMetadataForOtherBuilds(
        extension: KotlinMultiplatformExtension,
    ) {
        GlobalProjectStructureMetadataStorage.registerProjectStructureMetadata(extension.project) {
            extension.kotlinProjectStructureMetadata
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

    private fun processSpmDependencies(project: Project) {
        // FIXME: Replace "debug" with $triple and do "swift build --triple $triple"
        val dependencies = project.getExtension<ForeignDependencyContainer>(foreignDependenciesContainerExtension)!!

        val swiftDependencies = dependencies.dependencies.filter {
            it.str("provider") == "spm"
        }.map {
            val location: SpmDependencyLocation = if (it.hasKey("url")) {
                UrlLocation(it.str("url"), it.str("version"))
            } else {
                PathLocation(it.str("path"))
            }
            SwiftDependency(
                location = location,
                packageName = it.str("packageName"),
                products = it.list("products"),
            )
        }

        val packageSwiftGenerate = project.registerTask("packageSwiftGenerateTask", PackageSwiftGenerateTask::class.java) {
            it.dependencies.set(swiftDependencies)
        }

        val buildPackage = project.registerTask("buildSwiftPackage", BuildSwiftPackage::class.java) {
            it.dependsOn(packageSwiftGenerate) // ???
            it.packageSwiftFile.set(packageSwiftGenerate.flatMap { it.packageSwiftFile })
        }

        val spmInteropsProvider = project.registerTask("spmInterops", CInteropProcessWithMultipleLibraries::class.java) {
            it.dependsOn(buildPackage)
            it.packageSwiftFile.set(packageSwiftGenerate.flatMap { it.packageSwiftFile })
        }

        // FIXME: ???
        val interopKlibsProvider = project.fileTree(
            project.layout.buildDirectory.dir("spmInterop")
        )
        interopKlibsProvider.builtBy(spmInteropsProvider)
        interopKlibsProvider.include("**/*.klib")

        // FIXME: Use the sourceSet graph
        project.multiplatformExtension.supportedTargets().forEach { appleTarget ->
            appleTarget.compilations.getByName("main") { mainCompilation ->
                mainCompilation.compileTaskProvider.dependsOn(spmInteropsProvider)
                mainCompilation.compileTaskProvider.configure {
                    it.compilerOptions.freeCompilerArgs.addAll(
                        project.layout.buildDirectory.file(
                            "spm/.build/debug/libFakeLibrary.a"
                        ).mapToFile().map { listOf("-linker-option", it.path) },
                    )
                    it.compilerOptions.freeCompilerArgs.addAll(
                        project.layout.buildDirectory.dir(
                            "spm/.build/debug"
                        ).mapToFile().map { listOf("-linker-option", "-F${it.path}") },
                    )
                    it.compilerOptions.freeCompilerArgs.addAll(
                        spmInteropsProvider.flatMap {
                            it.targetsDescription.map {
                                val descriptions = Json.decodeFromString<CInteropProcessWithMultipleLibraries.TargetsDescription>(it.readText())
                                descriptions.targetToLibraries.values.flatten().mapTo(linkedSetOf()) { it }.map {
                                    listOf("-linker-option", "-L$it")
                                }.flatten() + descriptions.targetToFrameworks.values.flatten().mapTo(linkedSetOf()) { it }.map {
                                    listOf("-linker-option", "-F$it")
                                }.flatten()
                            }
                        }
                    )
                    it.compilerOptions.freeCompilerArgs.addAll(
                        project.provider {
                            val sdkName = when (appleTarget.konanTarget) {
                                is MACOS_X64, MACOS_ARM64 -> "macosx" // ??? catalyst ???
                                is IOS_X64, IOS_SIMULATOR_ARM64 -> "iphonesimulator"
                                is IOS_ARM64 -> "iphoneos"
                                is TVOS_X64, TVOS_SIMULATOR_ARM64 -> "appletvsimulator"
                                is TVOS_ARM64 -> "appletvos"
                                is WATCHOS_X64, WATCHOS_SIMULATOR_ARM64 -> "watchsimulator"
                                is WATCHOS_ARM64, WATCHOS_DEVICE_ARM64 -> "watchos"
                                else -> error("???")
                            }
                            // Get from dependencies pif instead?
                            val swiftLibrariesPath = Paths.get(runCommand(listOf("xcode-select", "-p")).dropLast(1)).resolve("Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${sdkName}")
                            val swiftLibrariesInSdkPath = Paths.get(runCommand(listOf("xcrun", "--sdk", sdkName, "--show-sdk-path")).dropLast(1)).resolve("usr/lib/swift")

                            listOf("-linker-option", "-L$swiftLibrariesPath", "-linker-option", "-L$swiftLibrariesInSdkPath")
                        }
                    )
                }
                // Filter by konan target name
                mainCompilation.compileDependencyFiles += interopKlibsProvider.filter {
                    it.nameWithoutExtension.endsWith(appleTarget.konanTarget.name)
                }

                project.launch {
                    project.commonizeCInteropTask()?.dependsOn(spmInteropsProvider)
                    project.commonizeCInteropTask()?.configure {
                        it.cinterops.add(
                            // Commonize everything in a single gist
                            project.provider {
                                CInteropCommonizerTask.CInteropGist(
                                    CInteropIdentifier(CInteropIdentifier.Scope.create(mainCompilation), "spmInterops"),
                                    appleTarget.konanTarget,
                                    project.provider { mainCompilation.kotlinSourceSets },
                                    project.provider { interopKlibsProvider.files.toList() },
                                    // ??? Dependencies ???
                                    project.files()
                                )
                            }
                        )
                    }
                }
            }
        }

        project.tasks.register("printKlibs") {
            it.dependsOn(spmInteropsProvider)
            it.doLast {
                println(interopKlibsProvider.files)
            }
        }
    }

    private fun processForeignDependencies(project: Project) {
        processSpmDependencies(project)
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

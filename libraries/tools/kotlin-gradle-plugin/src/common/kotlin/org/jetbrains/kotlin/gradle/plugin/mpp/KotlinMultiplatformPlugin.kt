/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.explicitApiModeAsCompilerArg
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy.SourceSetTree
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.ide.locateOrRegisterIdeResolveDependenciesTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin.Companion.sourceSetFreeCompilerArgsPropertyName
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.addBuildListenerForXcode
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.runDeprecationDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy.orNull
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.sources.awaitPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.checkSourceSetVisibilityRequirements
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWasmTargetPreset
import org.jetbrains.kotlin.gradle.targets.native.createFatFrameworks
import org.jetbrains.kotlin.gradle.targets.native.tasks.artifact.registerKotlinArtifactsExtension
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.File

class KotlinMultiplatformPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkGradleCompatibility("the Kotlin Multiplatform plugin", GradleVersion.version("6.0"))
        runDeprecationDiagnostics(project)

        project.plugins.apply(JavaBasePlugin::class.java)

        val kotlinMultiplatformExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

        setupDefaultPresets(project)
        customizeKotlinDependencies(project)
        configureSourceSets(project)

        // set up metadata publishing
        kotlinMultiplatformExtension.targetFromPreset(
            KotlinMetadataTargetPreset(project),
            METADATA_TARGET_NAME
        )
        project.registerKotlinArtifactsExtension()

        configurePublishingWithMavenPublish(project)

        kotlinMultiplatformExtension.targets.withType(AbstractKotlinTarget::class.java).all { applyUserDefinedAttributes(it) }

        // propagate compiler plugin options to the source set language settings
        setupAdditionalCompilerArguments(project)
        project.setupGeneralKotlinExtensionParameters()

        project.pluginManager.apply(ScriptingGradleSubplugin::class.java)

        exportProjectStructureMetadataForOtherBuilds(kotlinMultiplatformExtension)

        // Ensure that the instance is created and configured during apply
        project.kotlinIdeMultiplatformImport
        project.locateOrRegisterIdeResolveDependenciesTask()

        project.addBuildListenerForXcode()
        project.whenEvaluated { kotlinMultiplatformExtension.createFatFrameworks() }
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
        with(project.multiplatformExtension.presets) {
            add(KotlinJvmTargetPreset(project))
            add(KotlinJsTargetPreset(project).apply { irPreset = null })
            add(KotlinJsIrTargetPreset(project).apply { mixedMode = false })
            add(
                KotlinJsTargetPreset(project).apply {
                    irPreset = KotlinJsIrTargetPreset(project).apply { mixedMode = true }
                }
            )
            add(KotlinWasmTargetPreset(project))
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

        /* Create default 'dependsOn' to commonMain/commonTest (or even common{SourceSetTree}) */
        targets.all { target ->
            project.launchInStage(KotlinPluginLifecycle.Stage.FinaliseRefinesEdges) {
                /* Only setup default refines edges when no KotlinTargetHierarchy was applied */
                if (project.multiplatformExtension.internalKotlinTargetHierarchy.appliedDescriptors.isNotEmpty()) return@launchInStage

                target.compilations.forEach { compilation ->
                    val sourceSetTree = SourceSetTree.orNull(compilation) ?: return@forEach
                    val commonSourceSet = sourceSets.findByName(lowerCamelCaseName("common", sourceSetTree.name)) ?: return@forEach
                    compilation.defaultSourceSet.dependsOn(commonSourceSet)
                }
            }

            /* Report the platform to tbe build stats service */
            val targetName = if (target is KotlinNativeTarget)
                target.konanTarget.name
            else
                target.platformType.name
            KotlinBuildStatsService.getInstance()?.report(StringMetrics.MPP_PLATFORMS, targetName)
        }

        project.launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
            project.runProjectConfigurationHealthCheck {
                checkSourceSetVisibilityRequirements(project)
            }
        }
    }

    companion object {
        const val METADATA_TARGET_NAME = "metadata"

        internal fun sourceSetFreeCompilerArgsPropertyName(sourceSetName: String) =
            "kotlin.mpp.freeCompilerArgsForSourceSet.$sourceSetName"
    }
}

/**
 * The attributes attached to the targets and compilations need to be propagated to the relevant Gradle configurations:
 * 1. Output configurations of each target need the corresponding compilation's attributes (and, indirectly, the target's attributes)
 * 2. Resolvable configurations of each compilation need the compilation's attributes
 */
internal fun applyUserDefinedAttributes(target: AbstractKotlinTarget) {
    val project = target.project
    project.whenEvaluated {
        // To copy the attributes to the output configurations, find those output configurations and their producing compilations
        // based on the target's components:
        val outputConfigurationsWithCompilations = target.kotlinComponents.filterIsInstance<KotlinVariant>().flatMap { kotlinVariant ->
            kotlinVariant.usages.mapNotNull { usageContext ->
                project.configurations.findByName(usageContext.dependencyConfigurationName)?.let { configuration ->
                    configuration to usageContext.compilation
                }
            }
        }.toMutableList()

        val mainCompilation = target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        // Add usages of android library when its variants are grouped by flavor
        outputConfigurationsWithCompilations += target.kotlinComponents
            .filterIsInstance<JointAndroidKotlinTargetComponent>()
            .flatMap { variant -> variant.usages }
            .mapNotNull { usage ->
                val configuration = project.configurations.findByName(usage.dependencyConfigurationName) ?: return@mapNotNull null
                configuration to usage.compilation
            }

        outputConfigurationsWithCompilations.forEach { (configuration, compilation) ->
            copyAttributes(compilation.attributes, configuration.attributes)
        }

        target.compilations.all { compilation ->
            val compilationAttributes = compilation.attributes

            @Suppress("DEPRECATION")
            compilation.relatedConfigurationNames
                .mapNotNull { configurationName -> target.project.configurations.findByName(configurationName) }
                .forEach { configuration -> copyAttributes(compilationAttributes, configuration.attributes) }
        }

        // Copy to host-specific metadata elements configurations
        if (target is KotlinNativeTarget) {
            val hostSpecificMetadataElements = project.configurations.findByName(target.hostSpecificMetadataElementsConfigurationName)
            if (hostSpecificMetadataElements != null) {
                copyAttributes(from = target.attributes, to = hostSpecificMetadataElements.attributes)
            }
        }
    }
}

internal fun sourcesJarTask(compilation: KotlinCompilation<*>, componentName: String, artifactNameAppendix: String): TaskProvider<Jar> =
    sourcesJarTask(
        compilation.target.project,
        compilation.target.project.future {
            KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
            compilation.allKotlinSourceSets.associate { it.name to it.kotlin }
        },
        componentName,
        artifactNameAppendix
    )

private fun sourcesJarTask(
    project: Project,
    sourceSets: Future<Map<String, Iterable<File>>>,
    taskNamePrefix: String,
    artifactNameAppendix: String,
): TaskProvider<Jar> =
    sourcesJarTaskNamed(lowerCamelCaseName(taskNamePrefix, "sourcesJar"), taskNamePrefix, project, sourceSets, artifactNameAppendix)

internal fun sourcesJarTaskNamed(
    taskName: String,
    componentName: String,
    project: Project,
    sourceSets: Future<Map<String, Iterable<File>>>,
    artifactNameAppendix: String,
    componentTypeName: String = "target",
): TaskProvider<Jar> {
    project.locateTask<Jar>(taskName)?.let {
        return it
    }

    val result = project.registerTask<Jar>(taskName) { sourcesJar ->
        sourcesJar.archiveAppendix.set(artifactNameAppendix)
        sourcesJar.archiveClassifier.set("sources")
        sourcesJar.isPreserveFileTimestamps = false
        sourcesJar.isReproducibleFileOrder = true
        sourcesJar.group = BasePlugin.BUILD_GROUP
        sourcesJar.description = "Assembles a jar archive containing the sources of $componentTypeName '$componentName'."
    }

    result.configure {
        project.launch {
            sourceSets.await().forEach { (sourceSetName, sourceSetFiles) ->
                it.from(sourceSetFiles) { copySpec ->
                    copySpec.into(sourceSetName)
                    // Duplicates are coming from `SourceSets` that `sourceSet` depends on.
                    // Such dependency was added by Kotlin compilation.
                    // TODO: rethink approach for adding dependent `SourceSets` to Kotlin compilation `SourceSet`
                    copySpec.duplicatesStrategy = DuplicatesStrategy.WARN
                }
            }
        }
    }

    return result
}

internal fun Project.setupGeneralKotlinExtensionParameters() {
    project.launch {
        for (sourceSet in kotlinExtension.awaitSourceSets()) {
            val languageSettings = sourceSet.languageSettings
            if (languageSettings !is DefaultLanguageSettingsBuilder) continue

            val isMainSourceSet = sourceSet
                .internal
                .awaitPlatformCompilations()
                .any { SourceSetTree.orNull(it) == SourceSetTree.main }

            languageSettings.explicitApi = project.providers.provider {
                val explicitApiFlag = project.kotlinExtension.explicitApiModeAsCompilerArg()
                explicitApiFlag.takeIf { isMainSourceSet }
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

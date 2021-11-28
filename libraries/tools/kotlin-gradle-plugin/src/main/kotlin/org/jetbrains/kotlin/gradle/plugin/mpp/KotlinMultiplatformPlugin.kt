/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.util.ConfigureUtil
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.configureOrCreate
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin.Companion.sourceSetFreeCompilerArgsPropertyName
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.handleHierarchicalStructureFlagsMigration
import org.jetbrains.kotlin.gradle.plugin.sources.CleanupStaleSourceSetMetadataEntriesService
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.sources.SourceSetMetadataStorageForIde
import org.jetbrains.kotlin.gradle.plugin.sources.checkSourceSetVisibilityRequirements
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.File

class KotlinMultiplatformPlugin : Plugin<Project> {

    private class TargetFromPresetExtension(val targetsContainer: KotlinTargetsContainerWithPresets) {
        fun <T : KotlinTarget> fromPreset(preset: KotlinTargetPreset<T>, name: String, configureClosure: Closure<*>): T =
            fromPreset(preset, name) { ConfigureUtil.configure(configureClosure, this) }

        @JvmOverloads
        fun <T : KotlinTarget> fromPreset(preset: KotlinTargetPreset<T>, name: String, configureAction: T.() -> Unit = { }): T =
            targetsContainer.configureOrCreate(name, preset, configureAction)
    }

    override fun apply(project: Project) {
        checkGradleCompatibility("the Kotlin Multiplatform plugin", GradleVersion.version("6.0"))

        if (PropertiesProvider(project).mppStabilityNoWarn != true) {
            SingleWarningPerBuild.show(
                project,
                "Kotlin Multiplatform Projects are an Alpha feature. " +
                        "See: https://kotlinlang.org/docs/reference/evolution/components-stability.html. " +
                        "To hide this message, add '$STABILITY_NOWARN_FLAG=true' to the Gradle properties.\n"
            )
        }

        handleHierarchicalStructureFlagsMigration(project)

        project.plugins.apply(JavaBasePlugin::class.java)

        val targetsContainer = project.container(KotlinTarget::class.java)
        val kotlinMultiplatformExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val targetsFromPreset = TargetFromPresetExtension(kotlinMultiplatformExtension)

        kotlinMultiplatformExtension.apply {
            DslObject(targetsContainer).addConvention("fromPreset", targetsFromPreset)

            targets = targetsContainer
            addExtension("targets", targets)

            presets = project.container(KotlinTargetPreset::class.java)
            addExtension("presets", presets)

            defaultJsCompilerType = PropertiesProvider(project).jsCompiler
        }

        setupDefaultPresets(project)
        customizeKotlinDependencies(project)
        configureSourceSets(project)

        // set up metadata publishing
        targetsFromPreset.fromPreset(
            KotlinMetadataTargetPreset(project),
            METADATA_TARGET_NAME
        )
        configurePublishingWithMavenPublish(project)

        targetsContainer.withType(AbstractKotlinTarget::class.java).all { applyUserDefinedAttributes(it) }

        // propagate compiler plugin options to the source set language settings
        setupAdditionalCompilerArguments(project)
        project.setupGeneralKotlinExtensionParameters()

        project.pluginManager.apply(ScriptingGradleSubplugin::class.java)

        exportProjectStructureMetadataForOtherBuilds(project)

        SingleActionPerBuild.run(project.rootProject, "cleanup-processed-metadata") {
            if (isConfigurationCacheAvailable(project.gradle)) {
                BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(
                    project.gradle.sharedServices
                        .registerIfAbsent(
                            "cleanup-stale-sourceset-metadata",
                            CleanupStaleSourceSetMetadataEntriesService::class.java
                        ) {
                            CleanupStaleSourceSetMetadataEntriesService.configure(it, project)
                        }
                )
            } else {
                project.gradle.buildFinished {
                    SourceSetMetadataStorageForIde.cleanupStaleEntries(project)
                }
            }
        }
    }

    private fun exportProjectStructureMetadataForOtherBuilds(
        project: Project
    ) {
        GlobalProjectStructureMetadataStorage.registerProjectStructureMetadata(project) {
            checkNotNull(buildKotlinProjectStructureMetadata(project))
        }
    }

    private fun setupAdditionalCompilerArguments(project: Project) {
        // common source sets use the compiler options from the metadata compilation:
        val metadataCompilation =
            project.multiplatformExtension.metadata().compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        val primaryCompilationsBySourceSet by lazy { // don't evaluate eagerly: Android targets are not created at this point
            val allCompilationsForSourceSets = CompilationSourceSetUtil.compilationsBySourceSets(project).mapValues { (_, compilations) ->
                compilations.filter { it.target.platformType != KotlinPlatformType.common }
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
                    project.tasks.getByName(associatedCompilation.compileKotlinTaskName) as SourceTask
                }
            }
        }
    }

    fun setupDefaultPresets(project: Project) {
        with(project.multiplatformExtension.presets) {
            add(KotlinJvmTargetPreset(project))
            add(KotlinJsTargetPreset(project).apply { irPreset = null })
            add(KotlinJsIrTargetPreset(project, isWasm = false).apply { mixedMode = false })
            add(
                KotlinJsTargetPreset(project).apply {
                    irPreset = KotlinJsIrTargetPreset(project, isWasm = false)
                        .apply { mixedMode = true }
                }
            )
            add(KotlinJsIrTargetPreset(project, isWasm = true).apply { mixedMode = false })
            add(KotlinAndroidTargetPreset(project))
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
        val production = sourceSets.create(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)
        val test = sourceSets.create(KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME)

        targets.all { target ->
            target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)?.let { mainCompilation ->
                mainCompilation.defaultSourceSet.takeIf { it != production }?.dependsOn(production)
            }

            target.compilations.findByName(KotlinCompilation.TEST_COMPILATION_NAME)?.let { testCompilation ->
                testCompilation.defaultSourceSet.takeIf { it != test }?.dependsOn(test)
            }

            val targetName = if (target is KotlinNativeTarget)
                target.konanTarget.name
            else
                target.platformType.name
            KotlinBuildStatsService.getInstance()?.report(StringMetrics.MPP_PLATFORMS, targetName)
        }

        UnusedSourceSetsChecker.checkSourceSets(project)

        project.runProjectConfigurationHealthCheckWhenEvaluated {
            checkSourceSetVisibilityRequirements(project)
        }
    }

    companion object {
        const val METADATA_TARGET_NAME = "metadata"

        internal fun sourceSetFreeCompilerArgsPropertyName(sourceSetName: String) =
            "kotlin.mpp.freeCompilerArgsForSourceSet.$sourceSetName"

        internal const val STABILITY_NOWARN_FLAG = "kotlin.mpp.stability.nowarn"
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
        fun copyAttributes(from: AttributeContainer, to: AttributeContainer) {
            fun <T> copyAttribute(key: Attribute<T>, from: AttributeContainer, to: AttributeContainer) {
                to.attribute(key, from.getAttribute(key)!!)
            }

            from.keySet().forEach { key -> copyAttribute(key, from, to) }
        }

        // To copy the attributes to the output configurations, find those output configurations and their producing compilations
        // based on the target's components:
        val outputConfigurationsWithCompilations =
            target.kotlinComponents.filterIsInstance<KotlinVariant>().flatMap { kotlinVariant ->
                kotlinVariant.usages.mapNotNull { usageContext ->
                    project.configurations.findByName(usageContext.dependencyConfigurationName)?.let { configuration ->
                        configuration to usageContext.compilation
                    }
                }
            } + listOfNotNull(
                target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)?.let { mainCompilation ->
                    project.configurations.findByName(target.defaultConfigurationName)?.to(mainCompilation)
                }
            )

        outputConfigurationsWithCompilations.forEach { (configuration, compilation) ->
            copyAttributes(compilation.attributes, configuration.attributes)
        }

        target.compilations.all { compilation ->
            val compilationAttributes = compilation.attributes

            compilation.relatedConfigurationNames
                .mapNotNull { configurationName -> target.project.configurations.findByName(configurationName) }
                .forEach { configuration -> copyAttributes(compilationAttributes, configuration.attributes) }
        }
    }
}

internal fun sourcesJarTask(compilation: KotlinCompilation<*>, componentName: String?, artifactNameAppendix: String): TaskProvider<Jar> =
    sourcesJarTask(compilation.target.project, lazy { compilation.allKotlinSourceSets.associate { it.name to it.kotlin } }, componentName, artifactNameAppendix)

internal fun sourcesJarTask(
    project: Project,
    sourceSets: Lazy<Map<String, Iterable<File>>>,
    componentName: String?,
    artifactNameAppendix: String
): TaskProvider<Jar> = sourcesJarTaskNamed(lowerCamelCaseName(componentName, "sourcesJar"), project, sourceSets, artifactNameAppendix)

internal fun sourcesJarTaskNamed(
    taskName: String,
    project: Project,
    sourceSets: Lazy<Map<String, Iterable<File>>>,
    artifactNameAppendix: String
): TaskProvider<Jar> {
    project.locateTask<Jar>(taskName)?.let {
        return it
    }

    val result = project.registerTask<Jar>(taskName) { sourcesJar ->
        sourcesJar.archiveAppendix.set(artifactNameAppendix)
        sourcesJar.archiveClassifier.set("sources")
    }

    project.whenEvaluated {
        result.configure {
            sourceSets.value.forEach { (sourceSetName, sourceSetFiles) ->
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
    val sourceSetsInMainCompilation by lazy {
        CompilationSourceSetUtil.compilationsBySourceSets(project).filterValues { compilations ->
            compilations.any {
                // kotlin main compilation
                it.isMain()
                        // android compilation which is NOT in tested variant
                        || (it as? KotlinJvmAndroidCompilation)?.let { getTestedVariantData(it.androidVariant) == null } == true
            }
        }.keys
    }

    kotlinExtension.sourceSets.all { sourceSet ->
        (sourceSet.languageSettings as? DefaultLanguageSettingsBuilder)?.run {

            // Set ad-hoc free compiler args from the internal project property
            freeCompilerArgsProvider = project.provider {
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

                    val explicitApiState = project.kotlinExtension.explicitApi?.toCompilerArg()
                    // do not look into lazy set if explicitApiMode was not enabled
                    if (explicitApiState != null && sourceSet in sourceSetsInMainCompilation)
                        add(explicitApiState)
                }
            }
        }
    }
}

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
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin.Companion.sourceSetFreeCompilerArgsPropertyName
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.handleHierarchicalStructureFlagsMigration
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.registerDefaultVariantFactories
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.setupFragmentsMetadataForKpmModules
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.setupKpmModulesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.sources.checkSourceSetVisibilityRequirements
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWasmTargetPreset
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

        handleHierarchicalStructureFlagsMigration(project)

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
    }

    private fun exportProjectStructureMetadataForOtherBuilds(
        extension: KotlinMultiplatformExtension
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

internal fun sourcesJarTask(compilation: KotlinCompilation<*>, componentName: String, artifactNameAppendix: String): TaskProvider<Jar> =
    sourcesJarTask(
        compilation.target.project,
        lazy { compilation.allKotlinSourceSets.associate { it.name to it.kotlin } },
        componentName,
        artifactNameAppendix
    )

private fun sourcesJarTask(
    project: Project,
    sourceSets: Lazy<Map<String, Iterable<File>>>,
    taskNamePrefix: String,
    artifactNameAppendix: String
): TaskProvider<Jar> =
    sourcesJarTaskNamed(lowerCamelCaseName(taskNamePrefix, "sourcesJar"), taskNamePrefix, project, sourceSets, artifactNameAppendix)

internal fun sourcesJarTaskNamed(
    taskName: String,
    componentName: String,
    project: Project,
    sourceSets: Lazy<Map<String, Iterable<File>>>,
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
        kotlinExtension.sourceSets.filter { sourceSet ->
            sourceSet.internal.compilations.any {
                // kotlin main compilation
                it.isMain()
                        // android compilation which is NOT in tested variant
                        || (it as? KotlinJvmAndroidCompilation)?.let { getTestedVariantData(it.androidVariant) == null } == true
            }
        }
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

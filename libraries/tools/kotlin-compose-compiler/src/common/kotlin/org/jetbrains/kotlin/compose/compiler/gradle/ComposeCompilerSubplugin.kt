/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Provider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.compose.compiler.gradle.internal.ComposeWithAgpConfig
import org.jetbrains.kotlin.compose.compiler.gradle.model.builder.ComposeCompilerModelBuilder
import org.jetbrains.kotlin.gradle.plugin.*
import javax.inject.Inject

// Internal visibility could not be set until will properly support custom friendPaths:
// https://youtrack.jetbrains.com/issue/KT-65266/friendPathsSet-input-property-breaks-build-cache-reuse
/**
 * @suppress
 */
class ComposeCompilerGradleSubplugin
@Inject internal constructor(
    private val registry: ToolingModelBuilderRegistry,
) : KotlinCompilerPluginSupportPlugin {

    /**
     * @suppress
     */
    companion object {
        private const val COMPOSE_COMPILER_ARTIFACT_NAME = "kotlin-compose-compiler-plugin-embeddable"

        private val EMPTY_OPTION = SubpluginOption("", "")
    }

    private lateinit var composeExtension: ComposeCompilerGradlePluginExtension

    override fun apply(target: Project) {
        composeExtension = target.extensions.create("composeCompiler", ComposeCompilerGradlePluginExtension::class.java)
        registry.register(ComposeCompilerModelBuilder())
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return composeExtension.targetKotlinPlatforms.get().contains(kotlinCompilation.platformType)
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val composeWithAgpConfig = ComposeWithAgpConfig(project)

        // It is ok to check AGP existence without using `plugins.withId` wrapper as `applyToCompilation` will be called in `afterEvaluate`
        if (composeWithAgpConfig.isAgpComposeEnabled) {
            project.logger.log(LogLevel.INFO, "Detected Android Gradle Plugin compose compiler configuration")
            composeWithAgpConfig.agpComposeConfiguration?.resolutionStrategy?.dependencySubstitution {
                // entry.key is in the form of "group:module" (without a version), and
                // Gradle accepts that form.
                it.substitute(it.module("androidx.compose.compiler:compiler"))
                    .using(
                        it.module("org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:${project.getKotlinPluginVersion()}")
                    )
                    .withoutArtifactSelectors() // https://github.com/gradle/gradle/issues/5174#issuecomment-828558594
                    .because("Compose Compiler is now shipped as part of Kotlin distribution")
            }
        }

        val allPluginProperties = project.objects
            .listProperty(SubpluginOption::class.java)
            .apply {
                @Suppress("DEPRECATION")
                add(composeExtension.generateFunctionKeyMetaClasses.map { value ->
                    if (value) SubpluginOption("generateFunctionKeyMetaClasses", value.toString())
                    else EMPTY_OPTION
                })
                if (!composeWithAgpConfig.isDisableIncludeSourceInformationForAgp) {
                    add(composeExtension.includeSourceInformation.map {
                        SubpluginOption("sourceInformation", it.toString())
                    })
                }
                add(composeExtension.metricsDestination.map<SubpluginOption> {
                    val metricsSubDirectory = it.asFile
                        .resolve(kotlinCompilation.target.disambiguationClassifier ?: "")
                        .resolve(kotlinCompilation.compilationName)
                    FilesSubpluginOption(
                        "metricsDestination",
                        listOf(metricsSubDirectory)
                    )
                }.orElse(EMPTY_OPTION))
                add(composeExtension.reportsDestination.map<SubpluginOption> {
                    FilesSubpluginOption("reportsDestination", listOf(it.asFile))
                }.orElse(EMPTY_OPTION))

                @Suppress("DEPRECATION")
                add(composeExtension.stabilityConfigurationFile.map<SubpluginOption> {
                    FilesSubpluginOption("stabilityConfigurationPath", listOf(it.asFile))
                }.orElse(EMPTY_OPTION))

                addAll(composeExtension.stabilityConfigurationFiles.map { paths ->
                    paths.map { FilesSubpluginOption("stabilityConfigurationPath", listOf(it.asFile)) }
                }.orElse(emptyList()))

                add(composeExtension.includeTraceMarkers.map {
                    SubpluginOption("traceMarkersEnabled", it.toString())
                })

                @Suppress("DEPRECATION")
                addAll(
                    composeExtension.featureFlags
                        .zip(composeExtension.enableIntrinsicRemember) { featureFlags, intrinsicRemember ->
                            if (!intrinsicRemember && !featureFlags.contains(ComposeFeatureFlag.IntrinsicRemember.disabled())) {
                                featureFlags + ComposeFeatureFlag.IntrinsicRemember.disabled()
                            } else {
                                featureFlags
                            }
                        }
                        .zip(composeExtension.enableStrongSkippingMode) { featureFlags, strongSkippingMode ->
                            if (!strongSkippingMode && !featureFlags.contains(ComposeFeatureFlag.StrongSkipping.disabled())) {
                                featureFlags + ComposeFeatureFlag.StrongSkipping.disabled()
                            } else {
                                featureFlags
                            }
                        }
                        .zip(composeExtension.enableNonSkippingGroupOptimization) { featureFlags, nonSkippingGroupOptimization ->
                            if (nonSkippingGroupOptimization && !featureFlags.contains(ComposeFeatureFlag.OptimizeNonSkippingGroups)) {
                                featureFlags + ComposeFeatureFlag.OptimizeNonSkippingGroups
                            } else {
                                featureFlags
                            }
                        }
                        .map { flags ->
                            flags.map { SubpluginOption("featureFlag", it.toString()) }
                        }
                        .orElse(emptyList())
                )
            }

        return project.objects
            .listProperty(SubpluginOption::class.java)
            .value(allPluginProperties.map { pluginOptions ->
                pluginOptions.filter { it != EMPTY_OPTION }
            })
    }

    /** Get ID of the Kotlin Compiler plugin */
    override fun getCompilerPluginId() = "androidx.compose.compiler.plugins.kotlin"
    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(groupId = "org.jetbrains.kotlin", artifactId = COMPOSE_COMPILER_ARTIFACT_NAME)
}

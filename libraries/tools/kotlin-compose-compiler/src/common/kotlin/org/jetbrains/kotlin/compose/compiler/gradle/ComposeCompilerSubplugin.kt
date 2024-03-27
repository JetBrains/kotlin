/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.provider.Provider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.compose.compiler.gradle.model.builder.ComposeCompilerModelBuilder
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.plugin.*
import javax.inject.Inject

class ComposeCompilerGradleSubplugin
@Inject internal constructor(
    private val registry: ToolingModelBuilderRegistry,
) : KotlinCompilerPluginSupportPlugin {

    companion object {
        fun getComposeCompilerGradlePluginExtension(project: Project): ComposeCompilerGradlePluginExtension {
            return project.extensions.getByType(ComposeCompilerGradlePluginExtension::class.java)
        }

        private const val COMPOSE_COMPILER_ARTIFACT_NAME = "kotlin-compose-compiler-plugin-embeddable"

        private val EMPTY_OPTION = SubpluginOption("", "")
    }

    override fun apply(target: Project) {
        target.extensions.create("composeCompiler", ComposeCompilerGradlePluginExtension::class.java)
        registry.register(ComposeCompilerModelBuilder())
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val composeCompilerExtension = getComposeCompilerGradlePluginExtension(project)

        val kotlinExtensionConfiguration = try {
            project.configurations.getByName("kotlin-extension")
        } catch (e: UnknownConfigurationException) {
            null
        }
        if (kotlinExtensionConfiguration != null) println(kotlinExtensionConfiguration::class)

        kotlinExtensionConfiguration?.incoming?.beforeResolve {
            kotlinExtensionConfiguration.resolutionStrategy.dependencySubstitution {
                // entry.key is in the form of "group:module" (without a version), and
                // Gradle accepts that form.
                it.substitute(it.module("androidx.compose.compiler:compiler"))
                    .using(
                        it.module("org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:${KotlinCompilerVersion.getVersion()}")
                    )
                    .withoutArtifactSelectors() // https://github.com/gradle/gradle/issues/5174#issuecomment-828558594
                    .because("Compose Compiler is now shipped as part of Kotlin distribution")
            }
        }


        val allPluginProperties = project.objects
            .listProperty(SubpluginOption::class.java)
            .apply {
                add(composeCompilerExtension.generateFunctionKeyMetaClasses.map {
                    SubpluginOption("generateFunctionKeyMetaClasses", it.toString())
                })
                if (kotlinExtensionConfiguration == null) {
                    add(composeCompilerExtension.includeSourceInformation.map {
                        SubpluginOption("sourceInformation", it.toString())
                    })
                }
                add(composeCompilerExtension.metricsDestination.map {
                    SubpluginOption("metricsDestination", it.asFile.path)
                }.orElse(EMPTY_OPTION))
                add(composeCompilerExtension.reportsDestination.map {
                    SubpluginOption("reportsDestination", it.asFile.path)
                }.orElse(EMPTY_OPTION))
                add(composeCompilerExtension.enableIntrinsicRemember.map {
                    SubpluginOption("intrinsicRemember", it.toString())
                })
                add(composeCompilerExtension.enableNonSkippingGroupOptimization.map {
                    SubpluginOption("nonSkippingGroupOptimization", it.toString())
                })
                add(composeCompilerExtension.suppressKotlinVersionCompatibilityCheck.map {
                    SubpluginOption("suppressKotlinVersionCompatibilityCheck", it)
                }.orElse(EMPTY_OPTION))
                add(composeCompilerExtension.enableExperimentalStrongSkippingMode.map {
                    SubpluginOption("experimentalStrongSkipping", it.toString())
                })
                add(composeCompilerExtension.stabilityConfigurationFile.map {
                    SubpluginOption("stabilityConfigurationPath", it.asFile.path)
                }.orElse(EMPTY_OPTION))
                add(composeCompilerExtension.includeTraceMarkers.map {
                    SubpluginOption("traceMarkersEnabled", it.toString())
                })
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

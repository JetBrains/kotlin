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
    private val registry: ToolingModelBuilderRegistry
) : KotlinCompilerPluginSupportPlugin {

    companion object {
        fun getComposeCompilerGradlePluginExtension(project: Project): ComposeCompilerGradlePluginExtension {
            return project.extensions.getByType(ComposeCompilerGradlePluginExtension::class.java)
        }

        private const val COMPOSE_COMPILER_ARTIFACT_NAME = "kotlin-compose-compiler-plugin-embeddable"
    }

    override fun apply(target: Project) {
        target.extensions.create("compose_compiler", ComposeCompilerGradlePluginExtension::class.java)
        registry.register(ComposeCompilerModelBuilder())
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val composeCompilerExtension = project.extensions.getByType(ComposeCompilerGradlePluginExtension::class.java)

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

        return project.provider {
            val options = mutableListOf<SubpluginOption>()

            options += SubpluginOption("generateFunctionKeyMetaClasses", composeCompilerExtension.generateFunctionKeyMetaClasses.toString())
            if(kotlinExtensionConfiguration == null) // TODO: Is there a better way to check if AGP already set the option?
                options += SubpluginOption("sourceInformation", composeCompilerExtension.sourceInformation.toString())
            if(composeCompilerExtension.metricsDestination != null) options += SubpluginOption("metricsDestination", composeCompilerExtension.metricsDestination!!.toString())
            if(composeCompilerExtension.reportsDestination != null) options += SubpluginOption("reportsDestination", composeCompilerExtension.reportsDestination!!.toString())
            options += SubpluginOption("intrinsicRemember", composeCompilerExtension.intrinsicRemember.toString())
            options += SubpluginOption("nonSkippingGroupOptimization", composeCompilerExtension.nonSkippingGroupOptimization.toString())
            options += SubpluginOption("suppressKotlinVersionCompatibilityCheck", composeCompilerExtension.suppressKotlinVersionCompatibilityCheck.toString())
            options += SubpluginOption("experimentalStrongSkipping", composeCompilerExtension.experimentalStrongSkipping.toString())
            options += SubpluginOption("stabilityConfigurationPath", composeCompilerExtension.stabilityConfigurationPath)
            options += SubpluginOption("traceMarkersEnabled", composeCompilerExtension.traceMarkersEnabled.toString())

            options
        }
    }

    /** Get ID of the Kotlin Compiler plugin */
    override fun getCompilerPluginId() = "androidx.compose.compiler.plugins.kotlin"
    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(groupId = "org.jetbrains.kotlin", artifactId = COMPOSE_COMPILER_ARTIFACT_NAME)
}

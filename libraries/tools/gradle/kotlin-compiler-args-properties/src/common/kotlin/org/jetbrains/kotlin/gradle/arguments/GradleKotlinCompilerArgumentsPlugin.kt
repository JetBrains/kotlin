/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.arguments

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import javax.inject.Inject

class GradleKotlinCompilerArgumentsPlugin @Inject constructor(
    private val providerFactory: ProviderFactory
) : KotlinBasePlugin {
    override val pluginVersion = "test"

    override fun apply(project: Project) {
        val properties = KotlinTaskProperties(providerFactory)
        project.configureKotlinVersions(properties)
    }

    private fun Project.configureKotlinVersions(properties: KotlinTaskProperties) {
        plugins.withType<KotlinBasePlugin>().configureEach {
            configureExtension(properties)
            if (properties.kotlinOverrideUserValues.get()) {
                forceConfigureTask(properties)
            } else {
                configureTask(properties)
            }
        }
    }

    private fun Project.configureExtension(properties: KotlinTaskProperties) {
        extensions.projectCompilerOptions()?.let {
            configureKotlinOptions(
                properties,
                it,
                ignoreExtensionValue = true
            )
        }
    }

    private fun Project.configureTask(properties: KotlinTaskProperties) {
        tasks.withType<KotlinCompilationTask<*>>().configureEach {
            configureKotlinOptions(properties, it.compilerOptions)
        }
    }

    private fun Project.forceConfigureTask(properties: KotlinTaskProperties) {
        // Wrapping into afterEvaluate to reduce the number of exceptions trying to modify value from the user-script
        afterEvaluate {
            tasks.withType<KotlinCompilationTask<*>>().configureEach {
                configureKotlinOptions(properties, it.compilerOptions, true)
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    private fun ExtensionContainer.projectCompilerOptions(): KotlinCommonCompilerOptions? {
        val kotlinExtension = findByName("kotlin") ?: return null // Return on this plugin apply

        return when (kotlinExtension) {
            is KotlinJvmProjectExtension -> kotlinExtension.compilerOptions
            is KotlinAndroidProjectExtension -> kotlinExtension.compilerOptions
            is KotlinMultiplatformExtension -> kotlinExtension.compilerOptions
            else -> null // org.jetbrains.kotlin.js add "kotlin" as convention
        }
    }

    private fun Project.projectLevelLanguageVersion(): Provider<KotlinVersion> {
        return extensions.projectCompilerOptions()?.languageVersion ?: providers.provider { null }
    }

    private fun Project.projectLevelApiVersion(): Provider<KotlinVersion> {
        return extensions.projectCompilerOptions()?.apiVersion ?: providers.provider { null }
    }

    private fun Project.configureKotlinOptions(
        properties: KotlinTaskProperties,
        taskOptions: KotlinCommonCompilerOptions,
        shouldSetValue: Boolean = false,
        ignoreExtensionValue: Boolean = false,
    ) {
        taskOptions.languageVersion.configureValue(
            properties.kotlinLanguageVersion.run {
                if (!ignoreExtensionValue) orElse(projectLevelLanguageVersion()) else this
            },
            shouldSetValue
        )
        taskOptions.apiVersion.configureValue(
            properties.kotlinApiVersion.run {
                if (!ignoreExtensionValue) orElse(projectLevelApiVersion()) else this
            },
            shouldSetValue
        )
    }

    private fun <T : Any> Property<T>.configureValue(
        source: Provider<T>,
        shouldSetValue: Boolean
    ): Property<T> = if (shouldSetValue) {
        value(source)
    } else {
        convention(source)
    }
}

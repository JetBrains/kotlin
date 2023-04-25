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
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
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
            if (properties.kotlinOverrideUserValues.get()) {
                forceConfigureTask(properties)
            } else {
                configureTask(properties)
            }
        }
    }

    private fun Project.configureTask(properties: KotlinTaskProperties) {
        tasks.withType<KotlinCompilationTask<*>>().configureEach {
            configureKotlinOptions(properties, it.compilerOptions)
        }
    }

    private fun Project.forceConfigureTask(properties: KotlinTaskProperties) {
        // Wrapping into afterEvaluate to reduce amount of exception trying to modify value from the user-script
        afterEvaluate {
            tasks.withType<KotlinCompilationTask<*>>().configureEach {
                configureKotlinOptions(properties, it.compilerOptions, true)
            }
        }
    }

    private fun ExtensionContainer.projectCompilerOptions(): KotlinCommonCompilerOptions? =
        findByType(KotlinJvmProjectExtension::class.java)?.compilerOptions
            ?: findByType(KotlinAndroidProjectExtension::class.java)?.compilerOptions

    private fun Project.projectLevelLanguageVersion(): Provider<KotlinVersion> {
        return extensions.projectCompilerOptions()?.languageVersion ?: providers.provider { null }
    }

    private fun Project.projectLevelApiVersion(): Provider<KotlinVersion> {
        return extensions.projectCompilerOptions()?.apiVersion ?: providers.provider { null }
    }

    private fun Project.configureKotlinOptions(
        properties: KotlinTaskProperties,
        taskOptions: KotlinCommonCompilerOptions,
        shouldSetValue: Boolean = false
    ) {
        taskOptions.languageVersion.configureValue(properties.kotlinLanguageVersion.orElse(projectLevelLanguageVersion()), shouldSetValue)
        taskOptions.apiVersion.configureValue(properties.kotlinApiVersion.orElse(projectLevelApiVersion()), shouldSetValue)
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

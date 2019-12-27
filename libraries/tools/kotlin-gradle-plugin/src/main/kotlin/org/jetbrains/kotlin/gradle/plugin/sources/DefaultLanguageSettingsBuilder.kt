/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile

internal class DefaultLanguageSettingsBuilder : LanguageSettingsBuilder {
    private var languageVersionImpl: LanguageVersion? = null

    override var languageVersion: String?
        get() = languageVersionImpl?.versionString
        set(value) {
            languageVersionImpl = value?.let { versionString ->
                LanguageVersion.fromVersionString(versionString) ?: throw InvalidUserDataException(
                    "Incorrect language version. Expected one of: ${LanguageVersion.values().joinToString { "'${it.versionString}'" }}"
                )
            }
        }

    private var apiVersionImpl: ApiVersion? = null

    override var apiVersion: String?
        get() = apiVersionImpl?.versionString
        set(value) {
            apiVersionImpl = value?.let { versionString ->
                parseApiVersionSettings(versionString) ?: throw InvalidUserDataException(
                    "Incorrect API version. Expected one of: ${apiVersionValues.joinToString { "'${it.versionString}'" }}"
                )
            }
        }

    override var progressiveMode: Boolean = false

    private val enabledLanguageFeaturesImpl = mutableSetOf<LanguageFeature>()

    override val enabledLanguageFeatures: Set<String>
        get() = enabledLanguageFeaturesImpl.map { it.name }.toSet()

    override fun enableLanguageFeature(name: String) {
        val languageFeature = parseLanguageFeature(name) ?: throw InvalidUserDataException(
            "Unknown language feature '${name}'"
        )
        enabledLanguageFeaturesImpl += languageFeature
    }

    private val experimentalAnnotationsInUseImpl = mutableSetOf<String>()

    override val experimentalAnnotationsInUse: Set<String> = experimentalAnnotationsInUseImpl

    override fun useExperimentalAnnotation(name: String) {
        experimentalAnnotationsInUseImpl += name
    }

    /* A Kotlin task that is responsible for code analysis of the owner of this language settings builder. */
    var compilerPluginOptionsTask: Lazy<AbstractCompile?> = lazyOf(null)

    val compilerPluginArguments: List<String>?
        get() {
            val pluginOptionsTask = compilerPluginOptionsTask.value ?: return null
            return when (pluginOptionsTask) {
                is AbstractKotlinCompile<*> -> pluginOptionsTask.pluginOptions
                is AbstractKotlinNativeCompile<*> -> pluginOptionsTask.compilerPluginOptions
                else -> error("Unexpected task: $pluginOptionsTask")
            }.arguments
        }

    val compilerPluginClasspath: FileCollection?
        get() {
            val pluginClasspathTask = compilerPluginOptionsTask.value ?: return null
            return when (pluginClasspathTask) {
                is AbstractKotlinCompile<*> -> pluginClasspathTask.pluginClasspath
                is AbstractKotlinNativeCompile<*> -> pluginClasspathTask.compilerPluginClasspath ?: pluginClasspathTask.project.files()
                else -> error("Unexpected task: $pluginClasspathTask")
            }
        }
}

internal fun applyLanguageSettingsToKotlinTask(
    languageSettingsBuilder: LanguageSettingsBuilder,
    kotlinTask: org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>
) = with(kotlinTask.kotlinOptions) {
    languageVersion = languageVersion ?: languageSettingsBuilder.languageVersion
    apiVersion = apiVersion ?: languageSettingsBuilder.apiVersion

    if (languageSettingsBuilder.progressiveMode) {
        freeCompilerArgs += "-progressive"
    }

    languageSettingsBuilder.enabledLanguageFeatures.forEach { featureName ->
        freeCompilerArgs += "-XXLanguage:+$featureName"
    }

    languageSettingsBuilder.experimentalAnnotationsInUse.forEach { annotationName ->
        freeCompilerArgs += "-Xopt-in=$annotationName"
    }
}

private val apiVersionValues = ApiVersion.run { listOf(KOTLIN_1_0, KOTLIN_1_1, KOTLIN_1_2, KOTLIN_1_3) }

internal fun parseLanguageVersionSetting(versionString: String) = LanguageVersion.fromVersionString(versionString)
internal fun parseApiVersionSettings(versionString: String) = apiVersionValues.find { it.versionString == versionString }
internal fun parseLanguageFeature(featureName: String) = LanguageFeature.fromString(featureName)
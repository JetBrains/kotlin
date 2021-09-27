/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceTask
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
import org.jetbrains.kotlin.project.model.LanguageSettings
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics

internal class DefaultLanguageSettingsBuilder(@Transient private val project: Project) : LanguageSettingsBuilder {
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

    private val optInAnnotationsInUseImpl = mutableSetOf<String>()

    override val optInAnnotationsInUse: Set<String> = optInAnnotationsInUseImpl

    override val experimentalAnnotationsInUse: Set<String>
        get() {
            SingleWarningPerBuild.deprecation(
                project,
                "Kotlin language settings property",
                "experimentalAnnotationsInUse",
                "optInAnnotationsInUse"
            )
            return optInAnnotationsInUse
        }

    override fun optIn(annotationName: String) {
        optInAnnotationsInUseImpl += annotationName
    }

    override fun useExperimentalAnnotation(name: String) {
        SingleWarningPerBuild.deprecation(
            project,
            "Kotlin language settings function",
            "useExperimentalAnnotation",
            "optIn"
        )
        optIn(name)
    }

    /* A Kotlin task that is responsible for code analysis of the owner of this language settings builder. */
    @Transient // not needed during Gradle Instant Execution
    var compilerPluginOptionsTask: Lazy<SourceTask?> = lazyOf(null)

    val compilerPluginArguments: List<String>?
        get() {
            val pluginOptionsTask = compilerPluginOptionsTask.value ?: return null
            return when (pluginOptionsTask) {
                is AbstractKotlinCompile<*> -> pluginOptionsTask.pluginOptions
                is AbstractKotlinNativeCompile<*, *> -> pluginOptionsTask.compilerPluginOptions
                else -> error("Unexpected task: $pluginOptionsTask")
            }.arguments
        }

    val compilerPluginClasspath: FileCollection?
        get() {
            val pluginClasspathTask = compilerPluginOptionsTask.value ?: return null
            return when (pluginClasspathTask) {
                is AbstractKotlinCompile<*> -> pluginClasspathTask.pluginClasspath
                is AbstractKotlinNativeCompile<*, *> -> pluginClasspathTask.compilerPluginClasspath ?: pluginClasspathTask.project.files()
                else -> error("Unexpected task: $pluginClasspathTask")
            }
        }

    var freeCompilerArgsProvider: Provider<List<String>>? = null

    val freeCompilerArgs: List<String>
        get() = freeCompilerArgsProvider?.get().orEmpty()
}

internal fun applyLanguageSettingsToKotlinOptions(
    languageSettingsBuilder: LanguageSettings,
    kotlinOptions: KotlinCommonOptions
) = with(kotlinOptions) {
    languageVersion = languageVersion ?: languageSettingsBuilder.languageVersion
    apiVersion = apiVersion ?: languageSettingsBuilder.apiVersion
    
    val freeArgs = mutableListOf<String>().apply {
        if (languageSettingsBuilder.progressiveMode) {
            add("-progressive")
        }
    
        languageSettingsBuilder.enabledLanguageFeatures.forEach { featureName ->
            add("-XXLanguage:+$featureName")
        }
    
        languageSettingsBuilder.optInAnnotationsInUse.forEach { annotationName ->
            add("-opt-in=$annotationName")
        }

        if (languageSettingsBuilder is DefaultLanguageSettingsBuilder) {
            addAll(languageSettingsBuilder.freeCompilerArgs)
        }
    }

    freeCompilerArgs = freeCompilerArgs + freeArgs

    KotlinBuildStatsService.getInstance()?.apply {
        report(BooleanMetrics.KOTLIN_PROGRESSIVE_MODE, languageSettingsBuilder.progressiveMode)
        apiVersion?.also { v -> report(StringMetrics.KOTLIN_API_VERSION, v) }
        languageVersion?.also { v -> report(StringMetrics.KOTLIN_LANGUAGE_VERSION, v) }
    }
}

private val apiVersionValues = ApiVersion.run {
    listOf(
        KOTLIN_1_0,
        KOTLIN_1_1,
        KOTLIN_1_2,
        KOTLIN_1_3,
        KOTLIN_1_4,
        KOTLIN_1_5,
        KOTLIN_1_6,
        KOTLIN_1_7
    )
}

internal fun parseLanguageVersionSetting(versionString: String) = LanguageVersion.fromVersionString(versionString)
internal fun parseApiVersionSettings(versionString: String) = apiVersionValues.find { it.versionString == versionString }
internal fun parseLanguageFeature(featureName: String) = LanguageFeature.fromString(featureName)

/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.toSingleCompilerPluginOptions
import org.jetbrains.kotlin.gradle.utils.*
import javax.inject.Inject

internal open class DefaultLanguageSettingsBuilder @Inject constructor(
    @Transient private val project: Project
) : LanguageSettingsBuilder {
    internal val compilationCompilerOptions: CompletableFuture<KotlinCommonCompilerOptions> = CompletableFuture()

    override var languageVersion: String? = null
        get() = if (compilationCompilerOptions.isCompleted) {
            compilationCompilerOptions.getOrThrow().languageVersion.orNull?.version
        } else {
            field
        }
        set(value) {
            field = value
            project.launch {
                compilationCompilerOptions.await()
                    .languageVersion
                    .set(value?.let { KotlinVersion.fromVersion(it) })
            }
        }

    override var apiVersion: String? = null
        get() = if (compilationCompilerOptions.isCompleted) {
            compilationCompilerOptions.getOrThrow().apiVersion.orNull?.version
        } else {
            field
        }
        set(value) {
            field = value
            project.launch {
                compilationCompilerOptions.await()
                    .apiVersion
                    .set(value?.let { KotlinVersion.fromVersion(it) })
            }
        }

    override var progressiveMode: Boolean = false
        get() = if (compilationCompilerOptions.isCompleted) {
            compilationCompilerOptions.getOrThrow().progressiveMode.get()
        } else {
            field
        }
        set(value) {
            field = value
            project.launch {
                compilationCompilerOptions.await()
                    .progressiveMode
                    .set(value)
            }
        }

    private val enabledLanguageFeaturesField = mutableSetOf<String>()

    override val enabledLanguageFeatures: Set<String>
        get() = if (compilationCompilerOptions.isCompleted) {
            compilationCompilerOptions.getOrThrow()
                .freeCompilerArgs
                .get()
                .filter { it.startsWith("-XXLanguage:+") }
                .map { it.substringAfter("-XXLanguage:+") }
                .toSet()
        } else {
            enabledLanguageFeaturesField.toSet()
        }

    override fun enableLanguageFeature(name: String) {
        enabledLanguageFeaturesField.add(name)
        project.launch {
            compilationCompilerOptions.await()
                .freeCompilerArgs
                .add("-XXLanguage:+$name")
        }
    }

    private val optInAnnotationsInUseField = mutableSetOf<String>()

    override val optInAnnotationsInUse: Set<String>
        get() = if (compilationCompilerOptions.isCompleted) {
            compilationCompilerOptions.getOrThrow().optIn.get().toSet()
        } else {
            optInAnnotationsInUseField.toSet()
        }

    override fun optIn(annotationName: String) {
        optInAnnotationsInUseField.add(annotationName)
        project.launch {
            compilationCompilerOptions.await()
                .optIn.add(annotationName)
        }
    }

    /* A Kotlin task that is responsible for code analysis of the owner of this language settings builder. */
    @Transient // not needed during Gradle Instant Execution
    var compilerPluginOptionsTask: Lazy<AbstractKotlinCompileTool<*>?> = lazyOf(null)

    val compilerPluginArguments: List<String>?
        get() {
            val pluginOptionsTask = compilerPluginOptionsTask.value ?: return null
            return when (pluginOptionsTask) {
                is AbstractKotlinCompile<*> -> pluginOptionsTask.pluginOptions.toSingleCompilerPluginOptions()
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
        get() = if (compilationCompilerOptions.isCompleted) {
            compilationCompilerOptions.getOrThrow().freeCompilerArgs
        } else {
            field
        }
        set(value) {
            field = value
            // Checking if the provider has value as it overwrites the convention
            // https://github.com/gradle/gradle/issues/20266
            if (value != null && value.isPresent) {
                project.launch {
                    compilationCompilerOptions.await()
                        .freeCompilerArgs.addAll(value)
                }
            }
        }

    // Kept here for compatibility with IDEA Kotlin import. It relies on explicit api argument in `freeCompilerArgs` to enable related
    // inspections
    internal var explicitApi: Provider<String>? = null
        get() = if (compilationCompilerOptions.isCompleted) {
            val freeArgs = compilationCompilerOptions.getOrThrow()
                .freeCompilerArgs
                .get()
            freeArgs.find { it.startsWith("-Xexplicit-api") }?.let { project.providers.provider { it } }
        } else {
            field
        }
        set(value) {
            field = value
            // Checking if the provider has value as it overwrites the convention
            // https://github.com/gradle/gradle/issues/20266
            if (value != null && value.isPresent) {
                project.launch {
                    compilationCompilerOptions.await()
                        .freeCompilerArgs
                        .add(value)
                }
            }
        }

    val freeCompilerArgs: List<String>
        get() = if (compilationCompilerOptions.isCompleted) {
            compilationCompilerOptions.getOrThrow().freeCompilerArgs.get()
        } else {
            emptyList()
        }
}

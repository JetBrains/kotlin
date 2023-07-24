/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.toSingleCompilerPluginOptions
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.reflect.KProperty

internal open class DefaultLanguageSettingsBuilder @Inject constructor(
    project: Project,
    private val providersFactory: ProviderFactory
) : LanguageSettingsBuilder {
    // For shared source sets, it could be null if there is no associated Kotlin compilation task
    internal var compilationCompilerOptions: KotlinCommonCompilerOptions? = null

    // For shared source sets, it will contain additional compiler options for source sets, which depend on shared
    internal val dependentCompilerOptions: MutableSet<KotlinCommonCompilerOptions> = mutableSetOf()

    private val allCompilerOptions: Set<KotlinCommonCompilerOptions>
        get() = setOfNotNull(compilationCompilerOptions) + dependentCompilerOptions

    private val compilerOptionsAreAvailable = AtomicBoolean(false)

    init {
        // Kotlin source set could be created before related Kotlin compilation,
        // leaving a gap where none of the compilation options are available.
        // Additionally, 'allCompilerOptions' become fully available after 'AfterFinaliseRefinesEdges' state
        // when source sets relationship is finalized
        project.launchInStage(KotlinPluginLifecycle.Stage.FinaliseCompilations) {
            compilerOptionsAreAvailable.set(true)
            // Executing all pending compiler options changes
            if (languageVersionWrapper.isUpdated) languageVersion = languageVersionWrapper.get()
            if (apiVersionWrapper.isUpdated) apiVersion = apiVersionWrapper.get()
            if (progressiveModeWrapper.isUpdated) progressiveMode = progressiveModeWrapper.get()
            if (enabledLanguageFeaturesWrapper.isUpdated) enabledLanguageFeaturesWrapper.get()
                .forEach { enableLanguageFeature(it) }
            if (optInAnnotationsInUseWrapper.isUpdated) optInAnnotationsInUseWrapper.get()
                .forEach { optIn(it) }
            if (freeCompilerArgsProviderWrapper.isUpdated) freeCompilerArgsProvider = freeCompilerArgsProviderWrapper.get()
            if (explicitApiWrapper.isUpdated) explicitApi = explicitApiWrapper.get()
        }
    }

    private val languageVersionWrapper = ValueWrapper<String?>(null)

    override var languageVersion by DelayableValue(
        languageVersionWrapper,
        { allCompilerOptions.mapNotNull { it.languageVersion.orNull }.minByOrNull { it.ordinal }?.version },
        { value ->
            allCompilerOptions.forEach { compilerOptions ->
                compilerOptions.languageVersion.set(value?.let { KotlinVersion.fromVersion(it) })
            }
        }
    )

    private val apiVersionWrapper = ValueWrapper<String?>(null)

    override var apiVersion by DelayableValue(
        apiVersionWrapper,
        { allCompilerOptions.mapNotNull { it.apiVersion.orNull }.minByOrNull { it.ordinal }?.version },
        { value ->
            allCompilerOptions.forEach { compilerOptions ->
                compilerOptions.apiVersion.set(value?.let { KotlinVersion.fromVersion(it) })
            }
        }
    )

    private val progressiveModeWrapper = ValueWrapper(false)

    override var progressiveMode by DelayableValue(
        progressiveModeWrapper,
        { allCompilerOptions.map { it.progressiveMode.get() }.all { it } },
        { value ->
            allCompilerOptions.forEach { compilerOptions ->
                compilerOptions.progressiveMode.set(value)
            }
        }
    )

    private val enabledLanguageFeaturesWrapper = ValueWrapper<MutableSet<String>>(mutableSetOf())

    override val enabledLanguageFeatures: Set<String>
        get() = if (compilerOptionsAreAvailable.get()) {
            allCompilerOptions
                .flatMap { it.freeCompilerArgs.get() }
                .filter { it.startsWith("-XXLanguage:+") } // TODO: minimal common set
                .toSet()
        } else {
            enabledLanguageFeaturesWrapper.get().toSet()
        }

    override fun enableLanguageFeature(name: String) {
        if (compilerOptionsAreAvailable.get()) {
            allCompilerOptions.forEach { compilerOptions ->
                compilerOptions.freeCompilerArgs.add("-XXLanguage:+$name")
            }
        } else {
            enabledLanguageFeaturesWrapper.set(
                enabledLanguageFeaturesWrapper.get().apply { add(name) }
            )
        }
    }

    private val optInAnnotationsInUseWrapper = ValueWrapper<MutableSet<String>>(mutableSetOf())

    override val optInAnnotationsInUse: Set<String>
        get() = if (compilerOptionsAreAvailable.get()) {
            allCompilerOptions
                .flatMap { it.optIn.get() }
                .toSet()
        } else {
            optInAnnotationsInUseWrapper.get().toSet()
        }

    override fun optIn(annotationName: String) {
        if (compilerOptionsAreAvailable.get()) {
            allCompilerOptions.forEach { compilerOptions ->
                compilerOptions.optIn.add(annotationName)
            }
        } else {
            optInAnnotationsInUseWrapper.set(
                optInAnnotationsInUseWrapper.get().apply { add(annotationName) }
            )
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

    private val freeCompilerArgsProviderWrapper = ValueWrapper<Provider<List<String>>?>(null)

    var freeCompilerArgsProvider by DelayableValue(
        freeCompilerArgsProviderWrapper,
        {
            providersFactory.provider {
                allCompilerOptions.flatMap { it.freeCompilerArgs.get() }
            }
        },
        { value ->
            if (value != null && value.isPresent) {
                allCompilerOptions.forEach { compilerOptions ->
                    compilerOptions.freeCompilerArgs.addAll(value)
                }
            }
        }
    )

    private val explicitApiWrapper = ValueWrapper<Provider<String>?>(null)

    // Kept here for compatibility with IDEA Kotlin import. It relies on explicit api argument in `freeCompilerArgs` to enable related
    // inspections
    internal var explicitApi by DelayableValue(
        explicitApiWrapper,
        {
            providersFactory.provider {
                allCompilerOptions
                    .flatMap { it.freeCompilerArgs.get() }
                    .find { it.startsWith("-Xexplicit-api") }
            }
        },
        { value ->
            if (value != null && value.isPresent) {
                allCompilerOptions.forEach { compilerOptions ->
                    compilerOptions.freeCompilerArgs.add(value)
                }
            }
        }
    )

    val freeCompilerArgs: List<String> get() = allCompilerOptions.flatMap { it.freeCompilerArgs.get() }

    private class ValueWrapper<T : Any?>(
        initialValue: T
    ) {
        private var value: T = initialValue
        private var wasUpdated = false

        val isUpdated get() = wasUpdated

        fun get() = value
        fun set(value: T) {
            wasUpdated = true
            this.value = value
        }
    }

    private inner class DelayableValue<T>(
        private val backingFieldWrapper: ValueWrapper<T>,
        private val getValueWhenOptionsAvailable: () -> T,
        private val setValueWhenOptionsAvailable: (T) -> Unit
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return if (compilerOptionsAreAvailable.get()) {
                getValueWhenOptionsAvailable()
            } else {
                backingFieldWrapper.get()
            }
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            if (compilerOptionsAreAvailable.get()) {
                setValueWhenOptionsAvailable(value)
            } else {
                backingFieldWrapper.set(value)
            }
        }
    }
}

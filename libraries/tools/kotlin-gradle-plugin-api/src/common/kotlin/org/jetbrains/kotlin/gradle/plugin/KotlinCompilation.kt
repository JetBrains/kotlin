/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptionsDeprecated
import org.jetbrains.kotlin.gradle.dsl.KotlinCompileDeprecated
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.tooling.core.HasMutableExtras

interface KotlinCompilation<out T : KotlinCommonOptionsDeprecated> : Named,
    HasProject,
    HasMutableExtras,
    HasAttributes,
    HasKotlinDependencies {

    val target: KotlinTarget

    val compilationName: String

    val kotlinSourceSets: Set<KotlinSourceSet>

    val allKotlinSourceSets: Set<KotlinSourceSet>

    @Deprecated("Use defaultSourceSet.name instead", ReplaceWith("defaultSourceSet.name"))
    val defaultSourceSetName: String get() = defaultSourceSet.name

    val defaultSourceSet: KotlinSourceSet

    fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit)
    fun defaultSourceSet(configure: Action<KotlinSourceSet>) = defaultSourceSet { configure.execute(this) }

    val compileDependencyConfigurationName: String

    var compileDependencyFiles: FileCollection

    val runtimeDependencyConfigurationName: String?

    val runtimeDependencyFiles: FileCollection?

    val output: KotlinCompilationOutput

    val platformType get() = target.platformType

    val compileKotlinTaskName: String

    val compilerOptions: HasCompilerOptions<*>

    @Deprecated(
        message = "Accessing task instance directly is deprecated",
        replaceWith = ReplaceWith("compileTaskProvider")
    )
    val compileKotlinTask: KotlinCompileDeprecated<T>

    @Deprecated(
        message = "Replaced with compileTaskProvider",
        replaceWith = ReplaceWith("compileTaskProvider")
    )
    val compileKotlinTaskProvider: TaskProvider<out KotlinCompileDeprecated<T>>

    val compileTaskProvider: TaskProvider<out KotlinCompilationTask<*>>

    val kotlinOptions: T

    fun kotlinOptions(configure: T.() -> Unit) {
        @Suppress("DEPRECATION")
        configure(kotlinOptions)
    }

    fun kotlinOptions(configure: Action<@UnsafeVariance T>) {
        @Suppress("DEPRECATION")
        configure.execute(kotlinOptions)
    }

    fun attributes(configure: AttributeContainer.() -> Unit) = attributes.configure()
    fun attributes(configure: Action<AttributeContainer>) = attributes { configure.execute(this) }

    val compileAllTaskName: String

    companion object {
        const val MAIN_COMPILATION_NAME = "main"
        const val TEST_COMPILATION_NAME = "test"
    }

    fun source(sourceSet: KotlinSourceSet)

    fun associateWith(other: KotlinCompilation<*>)

    val associateWith: List<KotlinCompilation<*>>

    override fun getName(): String = compilationName

    override val relatedConfigurationNames: List<String>
        get() = super.relatedConfigurationNames + compileDependencyConfigurationName

    @Deprecated("Scheduled for removal with Kotlin 1.9. Use compilerOptions instead")
    val moduleName: String

    val disambiguatedName
        get() = target.disambiguationClassifier + name
}

@Deprecated("Scheduled for removal with Kotlin 1.9")
interface KotlinCompilationToRunnableFiles<T : KotlinCommonOptionsDeprecated> : KotlinCompilation<T> {
    override val runtimeDependencyConfigurationName: String

    override var runtimeDependencyFiles: FileCollection

    override val relatedConfigurationNames: List<String>
        get() = super.relatedConfigurationNames + runtimeDependencyConfigurationName
}

@Deprecated("Scheduled for removal with Kotlin 1.9")
@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "deprecation") // kept for compatibility
val <T : KotlinCommonOptionsDeprecated> KotlinCompilation<T>.runtimeDependencyConfigurationName: String?
    get() = (this as? KotlinCompilationToRunnableFiles<T>)?.runtimeDependencyConfigurationName

@Deprecated("Scheduled for removal with Kotlin 1.9")
interface KotlinCompilationWithResources<T : KotlinCommonOptionsDeprecated> : KotlinCompilation<T> {
    val processResourcesTaskName: String
}
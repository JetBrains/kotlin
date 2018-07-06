/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.sourceSetProvider
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.base.*
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCommonTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider

abstract class KotlinOnlyTargetPreset<T : KotlinCompilation>(
    protected val project: Project,
    private val instantiator: Instantiator,
    private val fileResolver: FileResolver,
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    protected val kotlinPluginVersion: String
) : KotlinTargetPreset<KotlinOnlyTarget<T>> {

    override fun createTarget(name: String): KotlinOnlyTarget<T> {
        val result = KotlinOnlyTarget<T>(project, platformType).apply {
            targetName = name
            disambiguationClassifier = name

            val compilationFactory = createCompilationFactoryForTarget(this)
            compilations = project.container(compilationFactory.itemClass, compilationFactory)
        }

        KotlinOnlyTargetConfigurator(buildOutputCleanupRegistry).configureTarget(project, result)

        result.compilations.all { compilation ->
            buildCompilationProcessor(compilation).run()
        }

        return result
    }

    protected abstract fun createCompilationFactoryForTarget(target: KotlinOnlyTarget<T>): KotlinCompilationFactory<T>
    protected abstract val platformType: KotlinPlatformType
    internal abstract fun buildCompilationProcessor(compilation: T): KotlinSourceSetProcessor<*>
}

class KotlinUniversalTargetPreset(
    project: Project,
    instantiator: Instantiator,
    fileResolver: FileResolver,
    buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    kotlinPluginVersion: String
) : KotlinOnlyTargetPreset<KotlinCommonCompilation>(
    project,
    instantiator,
    fileResolver,
    buildOutputCleanupRegistry,
    kotlinPluginVersion
) {
    override fun getName(): String = PRESET_NAME

    override fun createCompilationFactoryForTarget(target: KotlinOnlyTarget<KotlinCommonCompilation>)
            : KotlinCompilationFactory<KotlinCommonCompilation> =
        KotlinCommonCompilationFactory(project, target)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.common

    override fun buildCompilationProcessor(compilation: KotlinCommonCompilation): KotlinSourceSetProcessor<*> =
        KotlinCommonSourceSetProcessor(
            project,
            compilation,
            KotlinCommonTasksProvider(),
            kotlinPluginVersion
        )

    companion object {
        const val PRESET_NAME = "universal"
    }
}

class KotlinJvmTargetPreset(
    project: Project,
    instantiator: Instantiator,
    fileResolver: FileResolver,
    buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    kotlinPluginVersion: String
) : KotlinOnlyTargetPreset<KotlinJvmCompilation>(
    project,
    instantiator,
    fileResolver,
    buildOutputCleanupRegistry,
    kotlinPluginVersion
) {
    override fun getName(): String = PRESET_NAME

    override fun createCompilationFactoryForTarget(target: KotlinOnlyTarget<KotlinJvmCompilation>): KotlinCompilationFactory<KotlinJvmCompilation> =
        KotlinJvmCompilationFactory(project, target)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.jvm

    override fun buildCompilationProcessor(compilation: KotlinJvmCompilation): KotlinSourceSetProcessor<*> =
        Kotlin2JvmSourceSetProcessor(project, KotlinTasksProvider(), project.kotlinExtension.sourceSetProvider, compilation, kotlinPluginVersion)

    companion object {
        const val PRESET_NAME = "jvm"
    }
}

class KotlinJsTargetPreset(
    project: Project,
    instantiator: Instantiator,
    fileResolver: FileResolver,
    buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    kotlinPluginVersion: String
) : KotlinOnlyTargetPreset<KotlinJsCompilation>(
    project,
    instantiator,
    fileResolver,
    buildOutputCleanupRegistry,
    kotlinPluginVersion
) {
    override fun getName(): String = PRESET_NAME

    override fun createCompilationFactoryForTarget(target: KotlinOnlyTarget<KotlinJsCompilation>) =
        KotlinJsCompilationFactory(project, target)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.js

    override fun buildCompilationProcessor(compilation: KotlinJsCompilation): KotlinSourceSetProcessor<*> =
        Kotlin2JsSourceSetProcessor(project, Kotlin2JsTasksProvider(), compilation, kotlinPluginVersion)

    companion object {
        const val PRESET_NAME = "js"
    }
}
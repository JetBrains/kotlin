/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.JavaPlugin
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.AndroidTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KonanCompilerDownloadTask
import org.jetbrains.kotlin.gradle.tasks.KonanCompilerDownloadTask.Companion.KONAN_DOWNLOAD_TASK_NAME
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget

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

            val compilationFactory = createCompilationFactory(this)
            compilations = project.container(compilationFactory.itemClass, compilationFactory)
        }

        KotlinTargetConfigurator<T>(buildOutputCleanupRegistry).configureTarget(result)

        result.compilations.all { compilation ->
            buildCompilationProcessor(compilation).run()
        }

        return result
    }

    protected abstract fun createCompilationFactory(forTarget: KotlinOnlyTarget<T>): KotlinCompilationFactory<T>
    protected abstract val platformType: KotlinPlatformType
    internal abstract fun buildCompilationProcessor(compilation: T): KotlinSourceSetProcessor<*>
}

class KotlinMetadataTargetPreset(
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

    override fun createCompilationFactory(
        forTarget: KotlinOnlyTarget<KotlinCommonCompilation>
    ): KotlinCompilationFactory<KotlinCommonCompilation> =
        KotlinCommonCompilationFactory(forTarget)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.common

    override fun buildCompilationProcessor(compilation: KotlinCommonCompilation): KotlinSourceSetProcessor<*> =
        KotlinCommonSourceSetProcessor(
            project,
            compilation,
            KotlinTasksProvider(compilation.target.targetName),
            kotlinPluginVersion
        )

    companion object {
        const val PRESET_NAME = "metadata"
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

    override fun createCompilationFactory(forTarget: KotlinOnlyTarget<KotlinJvmCompilation>): KotlinCompilationFactory<KotlinJvmCompilation> =
        KotlinJvmCompilationFactory(forTarget)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.jvm

    override fun buildCompilationProcessor(compilation: KotlinJvmCompilation): KotlinSourceSetProcessor<*> =
        Kotlin2JvmSourceSetProcessor(project, KotlinTasksProvider(compilation.target.targetName), compilation, kotlinPluginVersion)

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

    override fun createCompilationFactory(forTarget: KotlinOnlyTarget<KotlinJsCompilation>) =
        KotlinJsCompilationFactory(project, forTarget)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.js

    override fun buildCompilationProcessor(compilation: KotlinJsCompilation): KotlinSourceSetProcessor<*> =
        Kotlin2JsSourceSetProcessor(project, KotlinTasksProvider(compilation.target.targetName), compilation, kotlinPluginVersion)

    companion object {
        const val PRESET_NAME = "js"
    }
}

class KotlinAndroidTargetPreset(
    private val project: Project,
    private val kotlinPluginVersion: String
) : KotlinTargetPreset<KotlinAndroidTarget> {

    override fun getName(): String = PRESET_NAME

    override fun createTarget(name: String): KotlinAndroidTarget {
        val result = KotlinAndroidTarget(name, project).apply {
            disambiguationClassifier = name
        }

        KotlinAndroidPlugin.applyToTarget(
            project, result, AndroidTasksProvider(name),
            kotlinPluginVersion
        )

        return result
    }

    companion object {
        const val PRESET_NAME = "android"
    }
}

class KotlinJvmWithJavaTargetPreset(
    private val project: Project,
    private val kotlinPluginVersion: String
): KotlinTargetPreset<KotlinWithJavaTarget> {

    override fun getName(): String = PRESET_NAME

    override fun createTarget(name: String): KotlinWithJavaTarget {
        project.plugins.apply(JavaPlugin::class.java)

        val target = KotlinWithJavaTarget(project, KotlinPlatformType.jvm, name).apply {
            disambiguationClassifier = name
        }

        AbstractKotlinPlugin.configureTarget(target) { compilation ->
            Kotlin2JvmSourceSetProcessor(project, KotlinTasksProvider(name), compilation, kotlinPluginVersion)
        }

        target.compilations.all { compilation ->
            // Set up dependency resolution using platforms:
            AbstractKotlinTargetConfigurator.defineConfigurationsForCompilation(compilation, target, project.configurations)
        }

        target.compilations.getByName("test").run {
            val main = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

            compileDependencyFiles = project.files(main.output, project.configurations.maybeCreate(compileDependencyConfigurationName))
            runtimeDependencyFiles = project.files(output, main.output, project.configurations.maybeCreate(runtimeDependencyConfigurationName))
        }

        return target
    }

    companion object {
        const val PRESET_NAME = "jvmWithJava"
    }
}

class KotlinNativeTargetPreset(
    private val name: String,
    val project: Project,
    val konanTarget: KonanTarget,
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry
) : KotlinTargetPreset<KotlinNativeTarget> {

    override fun getName(): String = name

    private fun createCompilerDownloadingTask() = with(project) {
        if (!hasProperty(KotlinNativeProjectProperty.KONAN_HOME)) {
            setProperty(KotlinNativeProjectProperty.KONAN_HOME, KonanCompilerDownloadTask.compilerDirectory)
            setProperty(KotlinNativeProjectProperty.DOWNLOAD_COMPILER, true)
        }
        tasks.maybeCreate(KONAN_DOWNLOAD_TASK_NAME, KonanCompilerDownloadTask::class.java)
    }

    override fun createTarget(name: String): KotlinNativeTarget {
        val result = KotlinNativeTarget(project, konanTarget).apply {
            targetName = name
            disambiguationClassifier = name

            val compilationFactory = KotlinNativeCompilationFactory(project, this)
            compilations = project.container(compilationFactory.itemClass, compilationFactory)
        }

        createCompilerDownloadingTask()
        KotlinNativeTargetConfigurator(buildOutputCleanupRegistry).configureTarget(result)
        return result
    }
}

internal val KonanTarget.presetName: String
    get() = when(this) {
        KonanTarget.ANDROID_ARM32 -> "androidNativeArm32"
        KonanTarget.ANDROID_ARM64 -> "androidNativeArm64"
        else -> lowerCamelCaseName(*this.name.split('_').toTypedArray())
    }

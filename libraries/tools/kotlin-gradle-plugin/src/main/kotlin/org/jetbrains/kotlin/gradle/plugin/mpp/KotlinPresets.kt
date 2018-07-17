/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.nativeplatform.test.tasks.RunTestExecutable
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.sourceSetProvider
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.base.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.setClassesDirCompatible
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.util.*

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

        KotlinOnlyTargetConfigurator(buildOutputCleanupRegistry).configureTarget(project, result)

        result.compilations.all { compilation ->
            buildCompilationProcessor(compilation).run()
        }

        return result
    }

    protected abstract fun createCompilationFactory(forTarget: KotlinOnlyTarget<T>): KotlinCompilationFactory<T>
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

    override fun createCompilationFactory(forTarget: KotlinOnlyTarget<KotlinCommonCompilation>)
            : KotlinCompilationFactory<KotlinCommonCompilation> =
        KotlinCommonCompilationFactory(project, forTarget)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.COMMON

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

    override fun createCompilationFactory(forTarget: KotlinOnlyTarget<KotlinJvmCompilation>): KotlinCompilationFactory<KotlinJvmCompilation> =
        KotlinJvmCompilationFactory(project, forTarget)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.JVM

    override fun buildCompilationProcessor(compilation: KotlinJvmCompilation): KotlinSourceSetProcessor<*> =
        Kotlin2JvmSourceSetProcessor(project, KotlinTasksProvider(), compilation, kotlinPluginVersion)

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
        get() = KotlinPlatformType.JS

    override fun buildCompilationProcessor(compilation: KotlinJsCompilation): KotlinSourceSetProcessor<*> =
        Kotlin2JsSourceSetProcessor(project, Kotlin2JsTasksProvider(), compilation, kotlinPluginVersion)

    companion object {
        const val PRESET_NAME = "js"
    }
}

class KotlinAndroidTargetPreset(
    private val project: Project,
    private val kotlinPluginVersion: String,
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry
) : KotlinTargetPreset<KotlinAndroidTarget> {

    override fun getName(): String = PRESET_NAME

    override fun createTarget(name: String): KotlinAndroidTarget {
        val result = KotlinAndroidTarget(project).apply {
            disambiguationClassifier = name

            val targetConfigurator = KotlinOnlyTargetConfigurator(buildOutputCleanupRegistry)
            compilations.all { compilation ->
                targetConfigurator.defineConfigurationsForCompilation(compilation, this@apply, project.configurations)
            }
        }

        KotlinAndroidPlugin.applyToTarget(
            project, result, project.kotlinExtension.sourceSetProvider,
            AndroidTasksProvider(), kotlinPluginVersion
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

        val target = KotlinWithJavaTarget(project, KotlinPlatformType.JVM, name)

        AbstractKotlinPlugin.configureTarget(target) { compilation ->
            Kotlin2JvmSourceSetProcessor(
                project,
                KotlinTasksProvider(),
                compilation as KotlinJvmCompilation,
                kotlinPluginVersion
            )
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
    val platformType: KotlinNativePlatformType,
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry
) : KotlinTargetPreset<KotlinNativeTarget> {

    override fun getName(): String = name

    private val Collection<*>.isDimensionVisible: Boolean
        get() = size > 1

    private fun createDimensionSuffix(dimensionName: String, multivalueProperty: Collection<*>): String =
        if (multivalueProperty.isDimensionVisible) {
            dimensionName.toLowerCase().capitalize()
        } else {
            ""
        }

    private fun Task.dependsOnCompilerDownloading() {
        val checkCompilerTask = project.tasks.maybeCreate(
            KonanCompilerDownloadTask.KONAN_DOWNLOAD_TASK_NAME,
            KonanCompilerDownloadTask::class.java
        )
        dependsOn(checkCompilerTask)
    }

    private fun createKlibCompilationTask(compilation: KotlinNativeCompilation) {
        val compileTask = project.tasks.create(
            compilation.compileKotlinTaskName,
            KotlinNativeCompile::class.java
        ).apply {
            this.compilation = compilation
            outputKind = CompilerOutputKind.LIBRARY
            group = BasePlugin.BUILD_GROUP
            description = "Compiles a klibrary from the '${compilation.name}' " +
                    "compilation for target '${compilation.platformType.name}'"

            outputFile.set {
                val targetSubDirectory = compilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
                val compilationName = compilation.compilationName
                val klibName = if (compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME)
                    project.name
                else
                    compilationName
                File(project.buildDir, "classes/kotlin/$targetSubDirectory$compilationName/$klibName.klib")
            }

            dependsOnCompilerDownloading()
        }

        compilation.output.tryAddClassesDir { project.files(compileTask.outputFile.asFile.get()) }

        project.tasks.getByName(compilation.compileAllTaskName).dependsOn(compileTask)
        if (compilation.compilationName == KotlinCompilation.MAIN_COMPILATION_NAME) {
            project.tasks.findByName(compilation.target.artifactsTaskName)?.dependsOn(compileTask)
            val apiElements = project.configurations.getByName(compilation.target.apiElementsConfigurationName)
            apiElements.artifacts.add(
                DefaultPublishArtifact(
                    compilation.name,
                    "klib",
                    "klib",
                    "klib",
                    Date(),
                    compileTask.outputFile.asFile.get(),
                    compileTask
                )
            )
        }
    }

    private fun createTestTask(compilation: KotlinNativeCompilation, testExecutableLinkTask: KotlinNativeCompile) {
        val taskName = lowerCamelCaseName("run", compilation.name, compilation.platformType.name)
        val testTask = project.tasks.create(taskName, RunTestExecutable::class.java).apply {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Executes Kotlin/Native unit tests from the '${compilation.name}' compilation " +
                    "for target '${compilation.platformType.name}'"

            val testExecutableProperty = testExecutableLinkTask.outputFile
            executable = testExecutableProperty.asFile.get().absolutePath
            // TODO: Provide a normal test path!
            outputDir = project.layout.buildDirectory.dir("test-results").get().asFile

            onlyIf { testExecutableProperty.asFile.get().exists() }
            inputs.file(testExecutableProperty)
            dependsOn(testExecutableLinkTask)
            dependsOnCompilerDownloading()
        }
    }

    // Called in whenEvaluated block.
    private fun createBinaryLinkTasks(compilation: KotlinNativeCompilation) {
        val konanTarget = compilation.target.konanTarget
        val buildTypes = compilation.buildTypes
        val availableOutputKinds = compilation.outputKinds.filter { it.availableFor(konanTarget) }

        // TODO: Consider using lockable set property.
        // TODO: Provide outgoing configurations somehow.
        for (buildType in compilation.buildTypes) {
            for (kind in availableOutputKinds) {
                val compilerOutputKind = kind.compilerOutputKind

                val compilationSuffix = compilation.name.takeIf { it != "main" }.orEmpty()
                val buildTypeSuffix = createDimensionSuffix(buildType.name, buildTypes)
                val targetSuffix = compilation.target.name
                val taskName = lowerCamelCaseName("link", compilationSuffix, buildTypeSuffix, kind.taskNameClassifier, targetSuffix)

                val linkTask = project.tasks.create(
                    taskName,
                    KotlinNativeCompile::class.java
                ).apply {
                    this.compilation = compilation
                    outputKind = compilerOutputKind
                    group = BasePlugin.BUILD_GROUP
                    description = "Links ${kind.description} from the '${compilation.name}' " +
                            "compilation for target '${compilation.platformType.name}'"

                    optimized = buildType.optimized
                    debuggable = buildType.debuggable

                    outputFile.set {
                        val targetSubDirectory = compilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
                        val compilationName = compilation.compilationName
                        val prefix = compilerOutputKind.prefix(konanTarget)
                        val suffix = compilerOutputKind.suffix(konanTarget)
                        File(project.buildDir, "bin/$targetSubDirectory$compilationName/$prefix$compilationName$suffix")
                    }

                    dependsOnCompilerDownloading()
                }

                project.tasks.maybeCreate(compilation.linkAllTaskName).dependsOn(linkTask)

                if (compilation.isTestCompilation &&
                    buildType == NativeBuildType.DEBUG &&
                    konanTarget == HostManager.host
                ) {
                    createTestTask(compilation, linkTask)
                }
            }
        }
    }

    override fun createTarget(name: String): KotlinNativeTarget {
        val result = KotlinNativeTarget(project, platformType).apply {
            targetName = name
            disambiguationClassifier = name

            val compilationFactory = KotlinNativeCompilationFactory(project, this)
            compilations = project.container(compilationFactory.itemClass, compilationFactory)
        }

        KotlinOnlyTargetConfigurator(buildOutputCleanupRegistry).configureTarget(project, result)

        // TODO: Move into KotlinNativeTargetConfigurator
        result.compilations.all { compilation ->
            createKlibCompilationTask(compilation)
            project.whenEvaluated {
                createBinaryLinkTasks(compilation)
            }
        }

        return result
    }
}
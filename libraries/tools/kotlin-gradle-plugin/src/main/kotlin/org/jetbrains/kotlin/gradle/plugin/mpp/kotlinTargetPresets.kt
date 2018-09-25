/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.IvyPatternRepositoryLayout
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.JavaPlugin
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.compilerRunner.KotlinNativeProjectProperty
import org.jetbrains.kotlin.compilerRunner.hasProperty
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.sources.applyLanguageSettingsToKotlinTask
import org.jetbrains.kotlin.gradle.tasks.AndroidTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.util.*

abstract class KotlinOnlyTargetPreset<T : KotlinCompilation>(
    protected val project: Project,
    private val instantiator: Instantiator,
    private val fileResolver: FileResolver,
    protected val buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    protected val kotlinPluginVersion: String
) : KotlinTargetPreset<KotlinOnlyTarget<T>> {

    protected open fun createKotlinTargetConfigurator(): KotlinTargetConfigurator<T> =
        KotlinTargetConfigurator(buildOutputCleanupRegistry, createDefaultSourceSets = true, createTestCompilation = true)

    override fun createTarget(name: String): KotlinOnlyTarget<T> {
        val result = KotlinOnlyTarget<T>(project, platformType).apply {
            targetName = name
            disambiguationClassifier = name

            val compilationFactory = createCompilationFactory(this)
            compilations = project.container(compilationFactory.itemClass, compilationFactory)
        }

        createKotlinTargetConfigurator().configureTarget(result)

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

    override fun createKotlinTargetConfigurator(): KotlinTargetConfigurator<KotlinCommonCompilation> =
        KotlinTargetConfigurator(buildOutputCleanupRegistry, createDefaultSourceSets = false, createTestCompilation = false)

    override fun createTarget(name: String): KotlinOnlyTarget<KotlinCommonCompilation> =
        super.createTarget(name).apply {
            val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            val commonMainSourceSet = project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)

            mainCompilation.source(commonMainSourceSet)

            project.afterEvaluate {
                // Since there's no default source set, apply language settings from commonMain:
                val compileKotlinMetadata = project.tasks.getByName(mainCompilation.compileKotlinTaskName) as KotlinCompile<*>
                applyLanguageSettingsToKotlinTask(commonMainSourceSet.languageSettings, compileKotlinMetadata)
            }
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
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    private val kotlinPluginVersion: String
) : KotlinTargetPreset<KotlinNativeTarget> {

    override fun getName(): String = name

    private fun setupNativeCompiler() = with(project) {
        if (!hasProperty(KotlinNativeProjectProperty.KONAN_HOME_OVERRIDE)) {
            NativeCompilerDownloader(this).downloadIfNeeded()
            logger.info("Kotlin/Native distribution: $konanHome")
        } else {
            logger.info("User-provided Kotlin/Native distribution: $konanHome")
        }
    }

    private fun setupKotlinNativeVirtualRepo(): Unit = with(project) {

        val repoAlreadyExists = this.repositories.asSequence()
            .filterIsInstance<IvyArtifactRepository>()
            .any { KOTLIN_NATIVE_FAKE_REPO_NAME == it.name }

        if (repoAlreadyExists) return

        this.repositories.ivy { repo ->
            repo.name = KOTLIN_NATIVE_FAKE_REPO_NAME
            repo.setUrl("file://$konanHome/klib")
            repo.layout("pattern") {
                val layout = it as IvyPatternRepositoryLayout
                layout.artifact("common/[artifact]")
                layout.artifact("platform/[classifier]/[artifact]")
            }
            repo.metadataSources {
                it.artifact()
            }
        }
    }

    private fun defaultLibs(target: KonanTarget? = null): List<Dependency> = with(project) {

        val relPath = if (target != null) "platform/${target.name}" else "common"

        file("$konanHome/klib/$relPath")
            .listFiles { file -> file.isDirectory }
            ?.sortedBy { dir -> dir.name.toLowerCase() }
            ?.map { dir ->
                dependencies.create(
                    mutableMapOf(
                        "group" to "Kotlin/Native",
                        "name" to dir.name,
                        "version" to getKotlinNativeLibraryVersion(dir)
                    ).also { dependencyNotation ->
                        if (target != null) dependencyNotation += "classifier" to target.name
                    }
                )
            } ?: emptyList()
    }

    override fun createTarget(name: String): KotlinNativeTarget {
        setupNativeCompiler()
        setupKotlinNativeVirtualRepo()

        val result = KotlinNativeTarget(project, konanTarget).apply {
            targetName = name
            disambiguationClassifier = name

            val compilationFactory = KotlinNativeCompilationFactory(project, this)
            compilations = project.container(compilationFactory.itemClass, compilationFactory)
        }

        KotlinNativeTargetConfigurator(buildOutputCleanupRegistry, kotlinPluginVersion).configureTarget(result)

        // Allow IDE to resolve the libraries provided by the compiler by adding them into dependencies.
        result.compilations.all { compilation ->
            val target = compilation.target.konanTarget
            compilation.dependencies {
                // First, put common libs:
                defaultLibs().forEach { implementation(it) }
                // Then, platform-specific libs:
                defaultLibs(target).forEach { implementation(it) }
            }
        }

        return result
    }

    companion object {
        private const val KOTLIN_NATIVE_FAKE_REPO_NAME = "Kotlin/Native default libraries"
    }
}

internal val KonanTarget.isCurrentHost: Boolean
    get() = this == HostManager.host

internal val KonanTarget.enabledOnCurrentHost
    get() = HostManager().isEnabled(this)

internal val KotlinNativeCompilation.isMainCompilation: Boolean
    get() = name == KotlinCompilation.MAIN_COMPILATION_NAME

private fun getKotlinNativeLibraryVersion(klibDir: File): String {
    val manifestFile = File(klibDir, "manifest")
    check(manifestFile.isFile) { "Manifest file not found for Kotlin/Native library: $klibDir" }

    val compilerVersion = Properties().also { it.load(manifestFile.bufferedReader()) }.getProperty("compiler_version")
    checkNotNull(compilerVersion) { "Compiler version not specified in manifest file: $manifestFile" }

    return KonanVersion.fromString(compilerVersion).toString()
}

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.IvyPatternRepositoryLayout
import org.gradle.api.artifacts.repositories.RepositoryLayout
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.JavaPlugin
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.compilerRunner.KotlinNativeProjectProperty
import org.jetbrains.kotlin.compilerRunner.hasProperty
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.sources.applyLanguageSettingsToKotlinTask
import org.jetbrains.kotlin.gradle.tasks.AndroidTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.util.*

abstract class KotlinOnlyTargetPreset<T : KotlinCompilation<*>>(
    protected val project: Project,
    private val instantiator: Instantiator,
    private val fileResolver: FileResolver,
    protected val kotlinPluginVersion: String
) : KotlinTargetPreset<KotlinOnlyTarget<T>> {

    protected open fun createKotlinTargetConfigurator(): KotlinTargetConfigurator<T> =
        KotlinTargetConfigurator(createDefaultSourceSets = true, createTestCompilation = true)

    override fun createTarget(name: String): KotlinOnlyTarget<T> {
        val result = KotlinOnlyTarget<T>(project, platformType).apply {
            targetName = name
            disambiguationClassifier = name
            preset = this@KotlinOnlyTargetPreset

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
    kotlinPluginVersion: String
) : KotlinOnlyTargetPreset<KotlinCommonCompilation>(
    project,
    instantiator,
    fileResolver,
    kotlinPluginVersion
) {
    override fun getName(): String = PRESET_NAME

    override fun createCompilationFactory(
        forTarget: KotlinOnlyTarget<KotlinCommonCompilation>
    ): KotlinCompilationFactory<KotlinCommonCompilation> =
        KotlinCommonCompilationFactory(forTarget)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.common

    override fun buildCompilationProcessor(compilation: KotlinCommonCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
        return KotlinCommonSourceSetProcessor(project, compilation, tasksProvider, kotlinPluginVersion)
    }

    companion object {
        const val PRESET_NAME = "metadata"
    }

    override fun createKotlinTargetConfigurator(): KotlinTargetConfigurator<KotlinCommonCompilation> =
        KotlinTargetConfigurator(createDefaultSourceSets = false, createTestCompilation = false)

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
    kotlinPluginVersion: String
) : KotlinOnlyTargetPreset<KotlinJvmCompilation>(
    project,
    instantiator,
    fileResolver,
    kotlinPluginVersion
) {
    override fun getName(): String = PRESET_NAME

    override fun createCompilationFactory(forTarget: KotlinOnlyTarget<KotlinJvmCompilation>): KotlinCompilationFactory<KotlinJvmCompilation> =
        KotlinJvmCompilationFactory(forTarget)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.jvm

    override fun buildCompilationProcessor(compilation: KotlinJvmCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
        return Kotlin2JvmSourceSetProcessor(project, tasksProvider, compilation, kotlinPluginVersion)
    }

    companion object {
        const val PRESET_NAME = "jvm"
    }
}

class KotlinJsTargetPreset(
    project: Project,
    instantiator: Instantiator,
    fileResolver: FileResolver,
    kotlinPluginVersion: String
) : KotlinOnlyTargetPreset<KotlinJsCompilation>(
    project,
    instantiator,
    fileResolver,
    kotlinPluginVersion
) {
    override fun getName(): String = PRESET_NAME

    override fun createCompilationFactory(forTarget: KotlinOnlyTarget<KotlinJsCompilation>) =
        KotlinJsCompilationFactory(project, forTarget)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.js

    override fun buildCompilationProcessor(compilation: KotlinJsCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
        return Kotlin2JsSourceSetProcessor(project, tasksProvider, compilation, kotlinPluginVersion)
    }

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
            preset = this@KotlinAndroidTargetPreset
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
) : KotlinTargetPreset<KotlinWithJavaTarget<KotlinJvmOptions>> {

    override fun getName(): String = PRESET_NAME

    override fun createTarget(name: String): KotlinWithJavaTarget<KotlinJvmOptions> {
        project.plugins.apply(JavaPlugin::class.java)

        val target = KotlinWithJavaTarget<KotlinJvmOptions>(project, KotlinPlatformType.jvm, name).apply {
            disambiguationClassifier = name
            preset = this@KotlinJvmWithJavaTargetPreset
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

            compileDependencyFiles = project.files(
                main.output.allOutputs,
                project.configurations.maybeCreate(compileDependencyConfigurationName)
            )
            runtimeDependencyFiles = project.files(
                output.allOutputs,
                main.output.allOutputs,
                project.configurations.maybeCreate(runtimeDependencyConfigurationName)
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
    val konanTarget: KonanTarget,
    private val kotlinPluginVersion: String
) : KotlinTargetPreset<KotlinNativeTarget> {

    init {
        // This is required to obtain Kotlin/Native home in CLion plugin:
        setupNativeHomePrivateProperty()
    }

    override fun getName(): String = name

    private fun setupNativeHomePrivateProperty() = with(project) {
        if (!hasProperty(KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY))
            extensions.extraProperties.set(KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY, konanHome)
    }

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

            fun IvyArtifactRepository.layoutCompatible(configureAction: (RepositoryLayout) -> Unit) =
                if (isGradleVersionAtLeast(5, 0)) {
                    val patternLayoutFunction = javaClass.getMethod("patternLayout", Action::class.java)
                    patternLayoutFunction(repo, Action<RepositoryLayout> { configureAction(it) })
                } else {
                    layout("pattern", configureAction)
                }

            repo.layoutCompatible {
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
            preset = this@KotlinNativeTargetPreset

            val compilationFactory = KotlinNativeCompilationFactory(project, this)
            compilations = project.container(compilationFactory.itemClass, compilationFactory)
        }

        KotlinNativeTargetConfigurator(kotlinPluginVersion).configureTarget(result)

        // Allow IDE to resolve the libraries provided by the compiler by adding them into dependencies.
        result.compilations.all { compilation ->
            val target = compilation.target.konanTarget
            // First, put common libs:
            defaultLibs().forEach {
                project.dependencies.add(compilation.compileDependencyConfigurationName, it)
            }
            // Then, platform-specific libs:
            defaultLibs(target).forEach {
                project.dependencies.add(compilation.compileDependencyConfigurationName, it)
            }
        }

        if (!konanTarget.enabledOnCurrentHost) {
            with(HostManager()) {
                val supportedHosts = enabledTargetsByHost.filterValues { konanTarget in it }.keys
                val supportedHostsString =
                    if (supportedHosts.size == 1)
                        "a ${supportedHosts.single()} host" else
                        "one of the host platforms: ${supportedHosts.joinToString(", ")}"
                project.logger.warn(
                    "Target '$name' for platform ${konanTarget} is ignored during build on this ${HostManager.host} machine. " +
                            "You can build it with $supportedHostsString."
                )
            }
        }

        return result
    }

    companion object {
        private const val KOTLIN_NATIVE_FAKE_REPO_NAME = "Kotlin/Native default libraries"
        private const val KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY = "konanHome"
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

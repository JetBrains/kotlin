/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

// Old package for compatibility

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.native.DisabledNativeTargetsReporter
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

abstract class AbstractKotlinNativeTargetPreset<T : KotlinNativeTarget>(
    private val name: String,
    val project: Project,
    val konanTarget: KonanTarget,
    protected val kotlinPluginVersion: String
) : KotlinTargetPreset<T> {

    init {
        // This is required to obtain Kotlin/Native home in CLion plugin:
        setupNativeHomePrivateProperty()
    }

    override fun getName(): String = name

    private fun setupNativeHomePrivateProperty() = with(project) {
        if (!hasProperty(KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY))
            extensions.extraProperties.set(KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY, konanHome)
    }

    private val isKonanHomeOverridden: Boolean
        get() = PropertiesProvider(project).nativeHome != null

    private fun setupNativeCompiler() = with(project) {
        if (!isKonanHomeOverridden) {
            NativeCompilerDownloader(this).downloadIfNeeded()
            logger.info("Kotlin/Native distribution: $konanHome")
        } else {
            logger.info("User-provided Kotlin/Native distribution: $konanHome")
        }
    }

    private fun nativeLibrariesList(directory: String) = with(project) {
        file("$konanHome/klib/$directory")
            .listFiles { file -> file.isDirectory }
            ?.sortedBy { dir -> dir.name.toLowerCase() }
    }

    // We declare default K/N dependencies (default and platform libraries) as files to avoid searching them in remote repos (see KT-28128).
    private fun defaultLibs(stdlibOnly: Boolean = false): List<Dependency> = with(project) {
        var filesList = nativeLibrariesList("common")
        if (stdlibOnly) {
            filesList = filesList?.filter { dir -> dir.name == "stdlib" }
        }

        filesList?.map { dir -> dependencies.create(files(dir)) } ?: emptyList()
    }

    private fun platformLibs(target: KonanTarget): List<Dependency> = with(project) {
        val filesList = nativeLibrariesList("platform/${target.name}")
        filesList?.map { dir -> dependencies.create(files(dir)) } ?: emptyList()
    }

    protected abstract fun createTargetConfigurator(): KotlinTargetConfigurator<T>

    protected abstract fun instantiateTarget(name: String): T

    override fun createTarget(name: String): T {
        setupNativeCompiler()

        val result = instantiateTarget(name).apply {
            targetName = name
            disambiguationClassifier = name
            preset = this@AbstractKotlinNativeTargetPreset

            val compilationFactory = KotlinNativeCompilationFactory(project, this)
            compilations = project.container(compilationFactory.itemClass, compilationFactory)
        }

        createTargetConfigurator().configureTarget(result)

        addDependenciesOnLibrariesFromDistribution(result)

        if (!konanTarget.enabledOnCurrentHost) {
            with(HostManager()) {
                val supportedHosts = enabledByHost.filterValues { konanTarget in it }.keys
                DisabledNativeTargetsReporter.reportDisabledTarget(project, result, supportedHosts)
            }
        }

        return result
    }

    // Allow IDE to resolve the libraries provided by the compiler by adding them into dependencies.
    private fun addDependenciesOnLibrariesFromDistribution(result: T) {
        result.compilations.all { compilation ->
            val target = compilation.konanTarget
            project.whenEvaluated {
                // First, put common libs:
                defaultLibs(!compilation.enableEndorsedLibs).forEach {
                    project.dependencies.add(compilation.compileDependencyConfigurationName, it)
                }
                // Then, platform-specific libs:
                platformLibs(target).forEach {
                    project.dependencies.add(compilation.compileDependencyConfigurationName, it)
                }
            }
        }

        // Add dependencies to stdlib-native for intermediate single-backend source-sets (like 'allNative')
        project.whenEvaluated {
            val compilationsBySourceSets = CompilationSourceSetUtil.compilationsBySourceSets(this)

            fun KotlinSourceSet.isIntermediateNativeSourceSet(): Boolean {
                val compilations = compilationsBySourceSets[this] ?: return false

                if (compilations.size <= 1) return false // intermediate source-sets participate at least in two compilations

                return compilations.all { it.target.platformType == KotlinPlatformType.native }
            }

            val stdlib = defaultLibs(stdlibOnly = true).single()

            project.kotlinExtension.sourceSets
                .filter { it.isIntermediateNativeSourceSet() }
                .forEach {
                    it.dependencies { implementation(stdlib) }
                }
        }
    }

    companion object {
        private const val KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY = "konanHome"
    }
}

open class KotlinNativeTargetPreset(name: String, project: Project, konanTarget: KonanTarget, kotlinPluginVersion: String) :
    AbstractKotlinNativeTargetPreset<KotlinNativeTarget>(name, project, konanTarget, kotlinPluginVersion) {

    override fun createTargetConfigurator(): KotlinTargetConfigurator<KotlinNativeTarget> =
        KotlinNativeTargetConfigurator(kotlinPluginVersion)

    override fun instantiateTarget(name: String): KotlinNativeTarget {
        return project.objects.newInstance(KotlinNativeTarget::class.java, project, konanTarget)
    }
}

open class KotlinNativeTargetWithTestsPreset(name: String, project: Project, konanTarget: KonanTarget, kotlinPluginVersion: String) :
    AbstractKotlinNativeTargetPreset<KotlinNativeTargetWithTests>(name, project, konanTarget, kotlinPluginVersion) {

    override fun createTargetConfigurator(): KotlinTargetConfigurator<KotlinNativeTargetWithTests> =
        KotlinNativeTargetWithTestsConfigurator(kotlinPluginVersion)

    override fun instantiateTarget(name: String): KotlinNativeTargetWithTests {
        return project.objects.newInstance(KotlinNativeTargetWithTests::class.java, project, konanTarget)
    }
}

internal val KonanTarget.isCurrentHost: Boolean
    get() = this == HostManager.host

internal val KonanTarget.enabledOnCurrentHost
    get() = HostManager().isEnabled(this)

internal val KotlinNativeCompilation.isMainCompilation: Boolean
    get() = name == KotlinCompilation.MAIN_COMPILATION_NAME

/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.native.DisabledNativeTargetsReporter
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

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

    override fun createTarget(name: String): KotlinNativeTarget {
        setupNativeCompiler()

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
            compilation.target.project.whenEvaluated {
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

        if (!konanTarget.enabledOnCurrentHost) {
            with(HostManager()) {
                val supportedHosts = enabledByHost.filterValues { konanTarget in it }.keys
                DisabledNativeTargetsReporter.reportDisabledTarget(project, result, supportedHosts)
            }
        }

        return result
    }

    companion object {
        private const val KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY = "konanHome"
    }
}

internal val KonanTarget.isCurrentHost: Boolean
    get() = this == HostManager.host

internal val KonanTarget.enabledOnCurrentHost
    get() = HostManager().isEnabled(this)

internal val KotlinNativeCompilation.isMainCompilation: Boolean
    get() = name == KotlinCompilation.MAIN_COMPILATION_NAME

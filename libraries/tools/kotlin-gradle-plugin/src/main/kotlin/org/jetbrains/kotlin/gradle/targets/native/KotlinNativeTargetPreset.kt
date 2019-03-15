/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
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
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.util.*

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

    // We declare default K/N dependencies as files to avoid searching them in remote repos (see KT-28128).
    private fun defaultLibs(target: KonanTarget? = null): List<Dependency> = with(project) {

        val relPath = if (target != null) "platform/${target.name}" else "common"

        file("$konanHome/klib/$relPath")
            .listFiles { file -> file.isDirectory }
            ?.sortedBy { dir -> dir.name.toLowerCase() }
            ?.map { dir -> dependencies.create(files(dir)) } ?: emptyList()
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
        private const val KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY = "konanHome"
    }
}

internal val KonanTarget.isCurrentHost: Boolean
    get() = this == HostManager.host

internal val KonanTarget.enabledOnCurrentHost
    get() = HostManager().isEnabled(this)

internal val KotlinNativeCompilation.isMainCompilation: Boolean
    get() = name == KotlinCompilation.MAIN_COMPILATION_NAME

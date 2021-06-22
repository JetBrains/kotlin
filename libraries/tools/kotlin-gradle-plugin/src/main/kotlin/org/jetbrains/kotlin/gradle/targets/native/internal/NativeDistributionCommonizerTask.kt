/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.resolveCommonizedDirectory
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.compilerRunner.GradleCliCommonizer
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCommonizerToolRunner
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.registerCommonizerClasspathConfigurationIfNecessary
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR
import java.io.File
import java.net.URLEncoder

internal open class NativeDistributionCommonizerTask : DefaultTask() {

    private val konanHome = project.file(project.konanHome)

    @get:Input
    internal val commonizerTargets: Set<SharedCommonizerTarget>
        get() = project.getAllCommonizerTargets()

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    @Suppress("unused") // Only for up-to-date checker. The directory with the original common libs.
    val originalCommonLibrariesDirectory = konanHome
        .resolve(KONAN_DISTRIBUTION_KLIB_DIR)
        .resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    @Suppress("unused") // Only for up-to-date checker. The directory with the original platform libs.
    val originalPlatformLibrariesDirectory = konanHome
        .resolve(KONAN_DISTRIBUTION_KLIB_DIR)
        .resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)

    @get:OutputDirectories
    @Suppress("unused") // Only for up-to-date checker.
    val outputDirectories: Set<File>
        get() {
            val rootOutputDirectory = getRootOutputDirectory()
            return commonizerTargets.map { target -> resolveCommonizedDirectory(rootOutputDirectory, target) }.toSet()
        }

    /*
    Ensures that only one CommonizerTask can run at a time.
    This is necessary because of the success-marker mechanism of this task.
    This is a phantom file: No one has the intention to actually create this output file.
    However, telling Gradle that all those tasks rely on the same output file will enforce
    non-parallel execution.
    */
    @get:OutputFile
    @Suppress("unused")
    val taskMutex: File = project.rootProject.file(".commonizer-phantom-output")

    @get:Internal
    internal val commonizerRunner = KotlinNativeCommonizerToolRunner(project)

    @get:Classpath
    @Suppress("unused") // Only for up-to-date checker.
    internal val commonizerClasspath: Set<File>
        get() = commonizerRunner.classpath

    @get:Input
    @Suppress("unused") // Only for up-to-date checker.
    internal val commonizerJvmArgs: List<String>
        get() = commonizerRunner.getCustomJvmArgs()

    @Internal
    internal fun getRootOutputDirectory(): File {
        val kotlinVersion = project.getKotlinPluginVersion()

        return project.file(konanHome)
            .resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR)
            .resolve(urlEncode(kotlinVersion))
    }

    @TaskAction
    protected fun run() {
        NativeDistributionCommonizationCache(project, GradleCliCommonizer(project)).commonizeNativeDistribution(
            konanHome = konanHome,
            outputDirectory = getRootOutputDirectory(),
            outputTargets = project.getAllCommonizerTargets(),
            logLevel = project.commonizerLogLevel
        )
    }

    init {
        project.registerCommonizerClasspathConfigurationIfNecessary()
    }
}

private fun Project.getAllCommonizerTargets(): Set<SharedCommonizerTarget> {
    return allprojects.flatMapTo(mutableSetOf<SharedCommonizerTarget>()) { project ->
        val kotlin = project.extensions.findByName("kotlin")
            ?.let { it as KotlinProjectExtension }
            ?.let { it as? KotlinMultiplatformExtension }
            ?: return@flatMapTo emptySet()

        kotlin.sourceSets
            .mapNotNull { sourceSet -> project.getCommonizerTarget(sourceSet) }
            .filterIsInstance<SharedCommonizerTarget>()
    }
}


private fun urlEncode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
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
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import java.io.File
import java.net.URLEncoder

internal open class NativeDistributionCommonizerTask : DefaultTask() {

    private val konanHome = project.file(project.konanHome)

    @get:Input
    internal val commonizerTargets: Set<SharedCommonizerTarget>
        get() = project.getAllCommonizerTargets()

    @get:Internal
    val outputDirectories: Set<File>
        get() {
            val rootOutputDirectory = getRootOutputDirectory()
            return commonizerTargets.map { target -> resolveCommonizedDirectory(rootOutputDirectory, target) }.toSet()
        }

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

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.compilerRunner.GradleCliCommonizer
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCommonizerToolRunner
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.registerCommonizerClasspathConfigurationIfNecessary
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import java.io.File
import java.net.URLEncoder

internal open class NativeDistributionCommonizerTask : DefaultTask() {

    private val konanHome = project.file(project.konanHome)

    @get:Input
    internal val commonizerTargets: Set<SharedCommonizerTarget>
        get() = project.collectAllSharedCommonizerTargetsFromBuild()

    @get:Internal
    internal val commonizerRunner = KotlinNativeCommonizerToolRunner(project)

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
        NativeDistributionCommonizationCache(project, GradleCliCommonizer(commonizerRunner)).commonizeNativeDistribution(
            konanHome = konanHome,
            outputDirectory = getRootOutputDirectory(),
            outputTargets = project.collectAllSharedCommonizerTargetsFromBuild(),
            logLevel = project.commonizerLogLevel
        )
    }

    init {
        project.registerCommonizerClasspathConfigurationIfNecessary()
    }
}

private fun Project.collectAllSharedCommonizerTargetsFromBuild(): Set<SharedCommonizerTarget> {
    return allprojects.flatMap { project -> project.collectAllSharedCommonizerTargetsFromProject() }.toSet()
}

private fun Project.collectAllSharedCommonizerTargetsFromProject(): Set<SharedCommonizerTarget> {
    return (project.multiplatformExtensionOrNull ?: return emptySet()).sourceSets
        .mapNotNull { sourceSet -> project.getCommonizerTarget(sourceSet) }
        .filterIsInstance<SharedCommonizerTarget>()
        .toSet()
}

private fun urlEncode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

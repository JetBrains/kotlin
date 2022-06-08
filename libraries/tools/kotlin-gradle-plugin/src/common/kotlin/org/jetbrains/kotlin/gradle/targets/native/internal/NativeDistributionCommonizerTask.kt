/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.compilerRunner.GradleCliCommonizer
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCommonizerToolRunner
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.registerCommonizerClasspathConfigurationIfNecessary
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import java.io.File
import java.net.URLEncoder

internal open class NativeDistributionCommonizerTask : DefaultTask() {

    private val konanHome = project.file(project.konanHome)

    private val commonizerTargets: Set<SharedCommonizerTarget> by lazy {
        project.collectAllSharedCommonizerTargetsFromBuild()
    }

    private val commonizer by lazy {
        NativeDistributionCommonizationCache(
            logger = project.logger,
            isCachingEnabled = project.kotlinPropertiesProvider.enableNativeDistributionCommonizationCache,
            commonizer = GradleCliCommonizer(KotlinNativeCommonizerToolRunner(project))
        )
    }

    @get:Internal
    internal val rootOutputDirectory: File by lazy {
        project.file(konanHome)
            .resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR)
            .resolve(URLEncoder.encode(project.getKotlinPluginVersion(), Charsets.UTF_8.name()))
    }

    @TaskAction
    protected fun run() {
        commonizer.commonizeNativeDistribution(
            konanHome = konanHome,
            outputDirectory = rootOutputDirectory,
            outputTargets = commonizerTargets,
            logLevel = project.commonizerLogLevel,
            additionalSettings = project.additionalCommonizerSettings,
        )
    }

    init {
        project.registerCommonizerClasspathConfigurationIfNecessary()
        outputs.upToDateWhen {
            commonizer.isUpToDate(konanHome, rootOutputDirectory, commonizerTargets)
        }
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

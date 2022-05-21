/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.compilerRunner.*
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
import javax.inject.Inject

internal open class NativeDistributionCommonizerTask
@Inject constructor (
    private val objectFactory: ObjectFactory,
    private val execOperations: ExecOperations,
) : DefaultTask() {

    private val konanHome = project.file(project.konanHome)

    @get:Input
    internal val commonizerTargets by lazy { project.collectAllSharedCommonizerTargetsFromBuild() }

    @get:Input
    internal val commonizerJvmArgs: List<String> = KotlinNativeCommonizerToolRunner.getCustomJvmArgs(project)

    @get:Input
    internal val isNativeDistributionCommonizationCacheEnabled = project.isNativeDistributionCommonizationCacheEnabled

    private val logLevel = project.commonizerLogLevel

    private val additionalSettings = project.additionalCommonizerSettings

    @get:Input
    internal val kotlinVersion by lazy { project.getKotlinPluginVersion() }

    @get:Classpath
    internal val classpath by lazy { KotlinNativeCommonizerToolRunner.buildClasspath(project) }

    @get:OutputDirectory
    internal val rootOutputDirectory: File = project.file {
        project.file(konanHome)
            .resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR)
            .resolve(urlEncode(kotlinVersion))
    }

//    internal val sensitiveOutputFiles = project.files(rootOutputDirectory).filter { file ->
//        file.endsWith(".lock")
//    }

    init {
        // Manually include properties that can't be fingerprinted by default Gradle @Input annotation
        inputs.property(NativeDistributionCommonizerTask::additionalSettings.name, additionalSettings.toString())
    }

    @TaskAction
    protected fun run() {
        val commonizerRunner = KotlinNativeCommonizerToolRunner(
            context = KotlinToolRunner.ExecutionContext.fromTaskContext(objectFactory, execOperations, logger),
            kotlinPluginVersion = kotlinVersion,
            classpathProvider = { classpath },
            customJvmArgs = commonizerJvmArgs
        )

        NativeDistributionCommonizationCache(
            isNativeDistributionCommonizationCacheEnabled = isNativeDistributionCommonizationCacheEnabled,
            logger = logger,
            commonizer = GradleCliCommonizer(commonizerRunner)
        ).commonizeNativeDistribution(
            konanHome = konanHome,
            outputDirectory = rootOutputDirectory,
            outputTargets = commonizerTargets,
            logLevel = logLevel,
            additionalSettings = additionalSettings,
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

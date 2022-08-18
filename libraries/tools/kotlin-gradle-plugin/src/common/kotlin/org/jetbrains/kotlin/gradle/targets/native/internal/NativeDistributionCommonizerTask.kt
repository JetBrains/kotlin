/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.compilerRunner.GradleCliCommonizer
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCommonizerToolRunner
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.chainedFinalizeValueOnRead
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import java.io.File
import java.net.URLEncoder
import javax.inject.Inject

internal open class NativeDistributionCommonizerTask
@Inject constructor(
    private val objectFactory: ObjectFactory,
    private val execOperations: ExecOperations,
) : DefaultTask() {

    private val konanHome = project.file(project.konanHome)

    private val commonizerTargets: Set<SharedCommonizerTarget> by lazy {
        project.collectAllSharedCommonizerTargetsFromBuild()
    }

    @get:Internal
    internal val kotlinPluginVersion: Property<String> = objectFactory
        .property<String>()
        .chainedFinalizeValueOnRead()

    @get:Classpath
    internal val commonizerClasspath: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Input
    internal val customJvmArgs: ListProperty<String> = objectFactory
        .listProperty<String>()
        .chainedFinalizeValueOnRead()

    private val runnerSettings: Provider<KotlinNativeCommonizerToolRunner.Settings> = kotlinPluginVersion
        .zip(customJvmArgs) { pluginVersion, customJvmArgs ->
            KotlinNativeCommonizerToolRunner.Settings(
                pluginVersion,
                commonizerClasspath.files,
                customJvmArgs
            )
        }

    private val logLevel = project.commonizerLogLevel

    private val additionalSettings = project.additionalCommonizerSettings

    @get:Internal
    internal val rootOutputDirectory: File = project.file {
        project.file(project.konanHome)
            .resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR)
            .resolve(URLEncoder.encode(project.getKotlinPluginVersion(), Charsets.UTF_8.name()))
    }

    private val commonizerCache = NativeDistributionCommonizerCache(
        outputDirectory = rootOutputDirectory,
        konanHome = konanHome,
        logger = logger,
        isCachingEnabled = project.kotlinPropertiesProvider.enableNativeDistributionCommonizationCache
    )

    @TaskAction
    protected fun run() {
        val commonizerRunner = KotlinNativeCommonizerToolRunner(
            context = KotlinToolRunner.GradleExecutionContext.fromTaskContext(objectFactory, execOperations, logger),
            settings = runnerSettings.get()
        )

        commonizerCache.writeCacheForUncachedTargets(commonizerTargets) { todoOutputTargets ->
            val commonizer = GradleCliCommonizer(commonizerRunner)
            /* Invoke commonizer with only 'to do' targets */
            commonizer.commonizeNativeDistribution(
                konanHome, rootOutputDirectory, todoOutputTargets, logLevel, additionalSettings
            )
        }
    }

    init {
        outputs.upToDateWhen {
            commonizerCache.isUpToDate(commonizerTargets)
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

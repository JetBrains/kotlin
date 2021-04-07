/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.commonizer.isAncestorOf
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCommonizerToolRunner
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

internal open class HierarchicalNativeDistributionCommonizerTask : DefaultTask() {

    private val konanHome = project.file(project.konanHome)

    @get:Input
    internal val rootCommonizerTargets: Set<SharedCommonizerTarget>
        get() = project.getRootCommonizerTargets()

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
    @Suppress("unused") // Only for up-to-date checker. The directory with the original platform libs.
    val outputDirectories: Set<File>
        get() = rootCommonizerTargets.map(::getRootOutputDirectory).toSet()

    @get:Internal
    internal val commonizerRunner = KotlinNativeCommonizerToolRunner(project)

    @get:Classpath
    @Suppress("unused") // Only for up-to-date checker. The directory with the original platform libs.
    internal val commonizerClasspath: Set<File>
        get() = commonizerRunner.classpath

    @get:Input
    @Suppress("unused") // Only for up-to-date checker. The directory with the original platform libs.
    internal val commonizerJvmArgs: List<String>
        get() = commonizerRunner.getCustomJvmArgs()

    internal fun getRootOutputDirectory(target: SharedCommonizerTarget): File {
        val kotlinVersion = checkNotNull(project.getKotlinPluginVersion()) { "Missing Kotlin Plugin version" }

        val discriminator = buildString {
            append(target.identityString)
            append("-")
            append(kotlinVersion.toLowerCase().base64)
        }

        return project.file(konanHome)
            .resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR)
            .resolve(discriminator)
    }

    @TaskAction
    protected fun run() {
        for (target in rootCommonizerTargets) {
            NativeDistributionCommonizationCache(commonizerRunner, getRootOutputDirectory(target))
                .runIfNecessary(getCommandLineArguments(target))
        }
    }

    private fun getCommandLineArguments(target: SharedCommonizerTarget): List<String> {
        return mutableListOf<String>().apply {
            this += "native-dist-commonize"
            this += "-distribution-path"
            this += konanHome.absolutePath
            this += "-output-path"
            this += getRootOutputDirectory(target).absolutePath
            this += "-output-commonizer-target"
            this += target.identityString
        }
    }
}

private fun Project.getRootCommonizerTargets(): Set<SharedCommonizerTarget> {
    val kotlin = multiplatformExtensionOrNull ?: return emptySet()
    val allTargets = kotlin.sourceSets
        .mapNotNull { sourceSet -> getCommonizerTarget(sourceSet) }
        .filterIsInstance<SharedCommonizerTarget>()
    return allTargets.filter { target -> allTargets.none { otherTarget -> otherTarget isAncestorOf target } }.toSet()
}

private val String.base64
    get() = base64Encoder.encodeToString(toByteArray(StandardCharsets.UTF_8))

private val base64Encoder = Base64.getEncoder().withoutPadding()


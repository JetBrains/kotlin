/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeFromToolchainProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.NoopKotlinNativeProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.UsesKotlinNativeBundleBuildService
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention
import java.io.File
import kotlin.collections.filterIsInstance
import kotlin.collections.orEmpty
import kotlin.io.resolve

@CacheableTask
abstract class KotlinNativeDownloadTask : DefaultTask(), UsesKotlinNativeBundleBuildService {

    @get:Nested
    internal val kotlinNativeProvider: Property<KotlinNativeProvider> = project.objects.propertyWithConvention<KotlinNativeProvider>(
        NoopKotlinNativeProvider(project)
    )

    @get:Internal
    abstract val konanHome: DirectoryProperty

    @get:OutputFile
    abstract val nativeDirectoryLocation: RegularFileProperty

    fun getPlatformDependencies(konanTargetName: String): Provider<Set<File>> {
        return nativeDirectoryLocation.map {
            val path = it.asFile.readText().let(::File)
            KonanDistribution(path).platformLibsDir.resolve(konanTargetName).listLibraryFiles().toSet()
        }
    }

    @TaskAction
    fun taskAction() {
        //force the download of the native bundle
        kotlinNativeProvider.get()
        nativeDirectoryLocation.get().asFile.writeText(konanHome.get().asFile.absolutePath)
    }
}

private const val kotlinNativeDownloadTaskName = "downloadKotlinNativeDistribution"

internal fun Project.getOrRegisterDownloadKotlinNativeDistributionTask(): TaskProvider<KotlinNativeDownloadTask> {
    return locateOrRegisterTask<KotlinNativeDownloadTask>(
        kotlinNativeDownloadTaskName,
        configureTask = {
            launch {
                val targets = multiplatformExtensionOrNull?.awaitTargets()?.toSet().orEmpty()
                kotlinNativeProvider.set(
                    KotlinNativeFromToolchainProvider(
                        project,
                        targets.filterIsInstance<KotlinNativeTarget>().map { it.konanTarget }.toSet(),
                        kotlinNativeBundleBuildService,
                        true
                    )
                )
                val koanDir = kotlinNativeProvider.flatMap { (it as KotlinNativeFromToolchainProvider).actualNativeHomeDirectory }
                konanHome.set(koanDir.get())
                nativeDirectoryLocation.set(layout.buildDirectory.file("konan.txt"))
            }
        }
    )

}

internal val Project.downloadKotlinNativeDistributionTask
    get() = tasks.named(kotlinNativeDownloadTaskName, KotlinNativeDownloadTask::class.java)

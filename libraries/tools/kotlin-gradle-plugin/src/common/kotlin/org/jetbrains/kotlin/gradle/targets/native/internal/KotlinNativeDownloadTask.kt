/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeFromToolchainProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.NoopKotlinNativeProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.UsesKotlinNativeBundleBuildService
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention
import java.io.File
import kotlin.io.resolve

@CacheableTask
abstract class KotlinNativeDownloadTask : DefaultTask(), UsesKotlinNativeBundleBuildService {

    @get:Nested
    internal val kotlinNativeProvider: Property<KotlinNativeProvider> = project.objects.propertyWithConvention<KotlinNativeProvider>(
        NoopKotlinNativeProvider(project)
    )

    @get:OutputDirectory
    abstract val nativeDirectory: DirectoryProperty

    fun getPlatformDependencies(konanTargetName: String): Provider<Set<File>> {
        return nativeDirectory.map {
            KonanDistribution(it.asFile).platformLibsDir.resolve(konanTargetName).listLibraryFiles().toSet()
        }
    }

    @TaskAction
    fun taskAction() {
        //force the download of the native bundle
        kotlinNativeProvider.get()
    }
}
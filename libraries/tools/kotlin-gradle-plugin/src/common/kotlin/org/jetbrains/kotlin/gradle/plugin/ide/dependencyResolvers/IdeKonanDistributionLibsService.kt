/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isCommonized
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionCommonizerTask
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import java.io.File

/**
 * Kotlin Native libraries can be reused between projects and even source sets (when they have the same [CommonizerTarget])
 * This build service caches set of [IDE dependencies][IdeaKotlinDependency] per [CommonizerTarget]
 */
internal abstract class IdeKonanDistributionLibsService : BuildService<IdeKonanDistributionLibsService.Params> {
    abstract class Params : BuildServiceParameters {
        abstract val kotlinNativeVersion: Property<String>
        abstract val konanDistributionLocation: Property<File>
    }

    private val cache = mutableMapOf<CommonizerTarget, Set<IdeaKotlinDependency>>()

    private val logger = Logging.getLogger(this.javaClass)

    private val konanDistribution: KonanDistribution by lazy { KonanDistribution(parameters.konanDistributionLocation.get()) }

    fun ideDependenciesOfLeafTarget(commonizerTarget: LeafCommonizerTarget): Set<IdeaKotlinDependency> {
        val konanTarget = commonizerTarget.konanTargetOrNull ?: return emptySet()
        return cache.getOrPut(commonizerTarget) {
            konanDistribution.platformLibsDir.resolve(konanTarget.name)
                .listFiles().orEmpty()
                .filter { it.isDirectory || it.extension == KLIB_FILE_EXTENSION }
                .mapNotNull { libraryFile ->
                    resolveNativeDistributionLibraryForIde(
                        libraryFile,
                        commonizerTarget,
                        parameters.kotlinNativeVersion.get(),
                        logger
                    )
                }.toSet()
        }
    }

    /**
     * [commonizedNativeDistributionKlibs] represents a list of commonized libraries for given [commonizerTarget]
     * Due to project isolation, each project has its own instance of [NativeDistributionCommonizerTask] but all these
     * tasks should point to the same commonized libraries. Since this is a Shared Build Service, so the first caller
     * will provide its instance of [commonizedNativeDistributionKlibs] that will be consumed as an actual list of commonized klibs.
     */
    fun ideDependenciesOfSharedTarget(
        commonizerTarget: SharedCommonizerTarget,
        commonizedNativeDistributionKlibs: Provider<List<File>>
    ): Set<IdeaKotlinDependency> {
        return cache.getOrPut(commonizerTarget) {
            commonizedNativeDistributionKlibs.get().mapNotNull { libraryFile ->
                resolveNativeDistributionLibraryForIde(
                    libraryFile,
                    commonizerTarget,
                    parameters.kotlinNativeVersion.get(),
                    logger
                )
            }.onEach { dependency -> dependency.isCommonized = true }.toSet()
        }
    }
}

internal fun Project.ideKonanDistributionLibsService() = gradle.registerClassLoaderScopedBuildService(
    IdeKonanDistributionLibsService::class
) {
    it.parameters.konanDistributionLocation.set(nativeProperties.actualNativeHomeDirectory)
    it.parameters.kotlinNativeVersion.set(nativeProperties.kotlinNativeVersion)
}

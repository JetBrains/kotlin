/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("SameParameterValue")

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.resolveCommonizedDirectory
import java.io.File
import java.io.ObjectInputStream
import java.io.Serializable

class NativeDistributionCommonizerCache(
    private val outputDirectory: File,
    private val konanHome: File,
    private val logger: Logger,
    private val isCachingEnabled: Boolean
) : Serializable {
    fun isUpToDate(
        outputTargets: Set<SharedCommonizerTarget>
    ): Boolean = lock.withLock {
        todoTargets(outputTargets)
    }.isEmpty()

    /**
     * Calls [writeCacheAction] for uncached targets and marks them as cached if it succeeds
     */
    fun writeCacheForUncachedTargets(
        outputTargets: Set<SharedCommonizerTarget>,
        writeCacheAction: (todoTargets: Set<SharedCommonizerTarget>) -> Unit
    ) = lock.withLock {
        val todoOutputTargets = todoTargets(outputTargets)
        if (todoOutputTargets.isEmpty()) return@withLock

        writeCacheAction(todoOutputTargets)

        todoOutputTargets
            .map { outputTarget -> resolveCommonizedDirectory(outputDirectory, outputTarget) }
            .filter { commonizedDirectory -> commonizedDirectory.isDirectory }
            .forEach { commonizedDirectory -> commonizedDirectory.resolve(".success").createNewFile() }
    }

    private fun todoTargets(
        outputTargets: Set<SharedCommonizerTarget>
    ): Set<SharedCommonizerTarget> {
        lock.checkLocked(outputDirectory)
        logInfo("Calculating cache state for $outputTargets")

        if (!isCachingEnabled) {
            logInfo("Cache disabled")
            return if (isMissingPlatformLibraries(outputTargets)) return emptySet()
            else outputTargets
        }

        val cachedOutputTargets = outputTargets
            .filter { outputTarget -> isCached(resolveCommonizedDirectory(outputDirectory, outputTarget)) }
            .onEach { outputTarget -> logInfo("Cache hit: $outputTarget already commonized") }
            .toSet()

        val todoOutputTargets = outputTargets - cachedOutputTargets

        if (todoOutputTargets.isEmpty() || isMissingPlatformLibraries(todoOutputTargets)) {
            logInfo("All available targets are commonized already - Nothing to do")
            if (todoOutputTargets.isNotEmpty()) {
                logInfo("Platforms cannot be commonized, because of missing platform libraries: $todoOutputTargets")
            }

            return emptySet()
        }

        return todoOutputTargets
    }

    private fun isMissingPlatformLibraries(
        missingOutputTargets: Set<CommonizerTarget>
    ): Boolean {
        // If all platform lib dirs are missing, we can also return fast from the cache without invoking
        //  the commonizer
        return missingOutputTargets.allLeaves()
            .map { target -> target.konanTarget }
            .map { konanTarget -> KonanDistribution(konanHome).platformLibsDir.resolve(konanTarget.name) }
            .none { platformLibsDir -> platformLibsDir.exists() }
    }

    private fun isCached(directory: File): Boolean {
        val successMarkerFile = directory.resolve(".success")
        return successMarkerFile.isFile
    }

    /**
     * Re-entrant lock implementation capable of locking a given output directory
     * even between multiple process (Gradle Daemons)
     */
    @Transient
    private var lock = NativeDistributionCommonizerLock(outputDirectory, ::logInfo)

    private fun logInfo(message: String) =
        logger.info("Native Distribution Commonization: $message")

    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
        lock = NativeDistributionCommonizerLock(outputDirectory, ::logInfo)
    }
}

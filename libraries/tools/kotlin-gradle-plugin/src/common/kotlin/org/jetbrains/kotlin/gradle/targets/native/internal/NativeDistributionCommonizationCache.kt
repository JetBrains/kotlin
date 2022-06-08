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
import java.io.FileOutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class NativeDistributionCommonizationCache(
    private val logger: Logger,
    private val isCachingEnabled: Boolean,
    private val commonizer: NativeDistributionCommonizer
) : NativeDistributionCommonizer {

    fun isUpToDate(
        konanHome: File, outputDirectory: File, outputTargets: Set<SharedCommonizerTarget>
    ): Boolean = lock.withLock(outputDirectory) {
        todoTargets(konanHome, outputDirectory, outputTargets)
    }.isEmpty()

    override fun commonizeNativeDistribution(
        konanHome: File,
        outputDirectory: File,
        outputTargets: Set<SharedCommonizerTarget>,
        logLevel: CommonizerLogLevel,
        additionalSettings: List<AdditionalCommonizerSetting<*>>,
    ): Unit = lock.withLock(outputDirectory) {
        val todoOutputTargets = todoTargets(konanHome, outputDirectory, outputTargets)
        if (todoOutputTargets.isEmpty()) return@withLock

        /* Invoke commonizer with only 'to do' targets */
        commonizer.commonizeNativeDistribution(
            konanHome, outputDirectory, todoOutputTargets, logLevel, additionalSettings
        )

        /* Mark targets as successfully commonized */
        todoOutputTargets
            .map { outputTarget -> resolveCommonizedDirectory(outputDirectory, outputTarget) }
            .filter { commonizedDirectory -> commonizedDirectory.isDirectory }
            .forEach { commonizedDirectory -> commonizedDirectory.resolve(".success").createNewFile() }
    }

    private fun todoTargets(
        konanHome: File, outputDirectory: File, outputTargets: Set<SharedCommonizerTarget>
    ): Set<SharedCommonizerTarget> {
        lock.checkLocked(outputDirectory)
        logInfo("Calculating cache state for $outputTargets")

        if (!isCachingEnabled) {
            logInfo("Cache disabled")
            return if (isMissingPlatformLibraries(konanHome, outputTargets)) return emptySet()
            else outputTargets
        }

        val cachedOutputTargets = outputTargets
            .filter { outputTarget -> isCached(resolveCommonizedDirectory(outputDirectory, outputTarget)) }
            .onEach { outputTarget -> logInfo("Cache hit: $outputTarget already commonized") }
            .toSet()

        val todoOutputTargets = outputTargets - cachedOutputTargets

        if (todoOutputTargets.isEmpty() || isMissingPlatformLibraries(konanHome, todoOutputTargets)) {
            logInfo("All available targets are commonized already - Nothing to do")
            if (todoOutputTargets.isNotEmpty()) {
                logInfo("Platforms cannot be commonized, because of missing platform libraries: $todoOutputTargets")
            }

            return emptySet()
        }

        return todoOutputTargets
    }

    private fun isCached(directory: File): Boolean {
        val successMarkerFile = directory.resolve(".success")
        return successMarkerFile.isFile
    }

    private fun isMissingPlatformLibraries(
        konanHome: File, missingOutputTargets: Set<CommonizerTarget>
    ): Boolean {
        // If all platform lib dirs are missing, we can also return fast from the cache without invoking
        //  the commonizer
        return missingOutputTargets.allLeaves()
            .map { target -> target.konanTarget }
            .map { konanTarget -> KonanDistribution(konanHome).platformLibsDir.resolve(konanTarget.name) }
            .none { platformLibsDir -> platformLibsDir.exists() }
    }

    /**
     * Re-entrant lock implementation capable of locking a given output directory
     * even between multiple process (Gradle Daemons)
     */
    private val lock = object {
        private val reentrantLock = ReentrantLock()
        private val lockedOutputDirectories = mutableSetOf<File>()

        fun <T> withLock(outputDirectory: File, action: () -> T): T {
            /* Enter intra-process wide lock */
            reentrantLock.withLock {
                if (outputDirectory in lockedOutputDirectories) {
                    /* Already acquired this directory and re-entered: We can just execute the action */
                    return action()
                }

                /* Lock output directory inter-process wide */
                outputDirectory.mkdirs()
                val lockfile = outputDirectory.resolve(".lock")
                logInfo("Acquire lock: ${lockfile.path} ...")
                FileOutputStream(outputDirectory.resolve(".lock")).use { stream ->
                    val lock = stream.channel.lock()
                    assert(lock.isValid)
                    return try {
                        logInfo("Lock acquired: ${lockfile.path}")
                        lockedOutputDirectories.add(outputDirectory)
                        action()
                    } finally {
                        lockedOutputDirectories.remove(outputDirectory)
                        lock.release()
                        logInfo("Lock released: ${lockfile.path}")
                    }
                }
            }
        }

        fun checkLocked(outputDirectory: File) {
            check(reentrantLock.isHeldByCurrentThread) {
                "Expected lock to be held by current thread ${Thread.currentThread().name}"
            }

            check(outputDirectory in lockedOutputDirectories) {
                "Expected $outputDirectory to be locked. Locked directories: $lockedOutputDirectories"
            }
        }
    }

    private fun logInfo(message: String) =
        logger.info("Native Distribution Commonization: $message")
}

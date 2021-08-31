/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("SameParameterValue")

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.resolveCommonizedDirectory
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import java.io.File
import java.io.FileOutputStream

internal val Project.isNativeDistributionCommonizationCacheEnabled: Boolean
    get() = PropertiesProvider(this).enableNativeDistributionCommonizationCache

internal class NativeDistributionCommonizationCache(
    private val project: Project,
    private val commonizer: NativeDistributionCommonizer
) : NativeDistributionCommonizer {


    override fun commonizeNativeDistribution(
        konanHome: File,
        outputDirectory: File,
        outputTargets: Set<SharedCommonizerTarget>,
        logLevel: CommonizerLogLevel
    ) {
        if (!project.isNativeDistributionCommonizationCacheEnabled) {
            logInfo("Cache disabled")
        }

        withLock(outputDirectory) {
            val cachedOutputTargets = outputTargets
                .filter { outputTarget -> isCached(resolveCommonizedDirectory(outputDirectory, outputTarget)) }
                .onEach { outputTarget -> logInfo("Cache hit: $outputTarget already commonized") }
                .toSet()

            val enqueuedOutputTargets = if (project.isNativeDistributionCommonizationCacheEnabled) outputTargets - cachedOutputTargets
            else outputTargets

            if (canReturnFast(konanHome, enqueuedOutputTargets)) {
                logInfo("All available targets are commonized already - Nothing to do")
                return
            }

            enqueuedOutputTargets
                .map { outputTarget -> resolveCommonizedDirectory(outputDirectory, outputTarget) }
                .forEach { commonizedDirectory -> if (commonizedDirectory.exists()) commonizedDirectory.deleteRecursively() }

            commonizer.commonizeNativeDistribution(
                konanHome, outputDirectory, enqueuedOutputTargets, logLevel
            )

            enqueuedOutputTargets
                .map { outputTarget -> resolveCommonizedDirectory(outputDirectory, outputTarget) }
                .filter { commonizedDirectory -> commonizedDirectory.isDirectory }
                .forEach { commonizedDirectory -> commonizedDirectory.resolve(".success").createNewFile() }
        }
    }

    private fun isCached(directory: File): Boolean {
        val successMarkerFile = directory.resolve(".success")
        return successMarkerFile.isFile
    }

    private fun canReturnFast(
        konanHome: File, missingOutputTargets: Set<CommonizerTarget>
    ): Boolean {
        if (missingOutputTargets.isEmpty()) return true

        // If all platform lib dirs are missing, we can also return fast from the cache without invoking
        //  the commonizer
        return missingOutputTargets.allLeaves()
            .map { target -> target.konanTarget }
            .map { konanTarget -> KonanDistribution(konanHome).platformLibsDir.resolve(konanTarget.name) }
            .none { platformLibsDir -> platformLibsDir.exists() }
    }

    private inline fun <T> withLock(outputDirectory: File, action: () -> T): T {
        outputDirectory.mkdirs()
        val lockfile = outputDirectory.resolve(".lock")
        logInfo("Acquire lock: ${lockfile.path} ...")
        FileOutputStream(outputDirectory.resolve(".lock")).use { stream ->
            val lock = stream.channel.lock()
            assert(lock.isValid)
            return try {
                logInfo("Lock acquired: ${lockfile.path}")
                action()
            } finally {
                lock.release()
                logInfo("Lock released: ${lockfile.path}")
            }
        }
    }

    private fun logInfo(message: String) = project.logger.info("${Logging.prefix}: $message")

    private object Logging {
        const val prefix = "Native Distribution Commonization"
    }
}


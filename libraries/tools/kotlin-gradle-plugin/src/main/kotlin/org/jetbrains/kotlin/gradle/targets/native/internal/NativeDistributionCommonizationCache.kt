/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.compilerRunner.KotlinToolRunner
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import java.io.File

internal val Project.isNativeDistributionCommonizationCacheEnabled: Boolean
    get() = PropertiesProvider(this).enableNativeDistributionCommonizationCache

internal class NativeDistributionCommonizationCache(
    private val runner: KotlinToolRunner,
    private val outputDirectory: File
) {
    private val successMarker = outputDirectory.resolve(".commonized")

    fun runIfNecessary(args: List<String>) {
        if (!isCached(args)) {
            run(args)
        }
    }

    private fun isCached(args: List<String>): Boolean {
        if (!runner.project.isNativeDistributionCommonizationCacheEnabled) {
            logInfo("Cache disabled")
            return false
        }

        if (successMarker.exists() && successMarker.isFile) {
            if (successMarker.readText() == successMarkerText(args)) {
                logInfo("Cache hit for ${outputDirectory.path}")
                return true
            } else {
                logQuiet("Cache miss. Different arguments for ${outputDirectory.path}")
                successMarker.delete()
            }
        }

        return false
    }

    private fun run(args: List<String>) {
        outputDirectory.deleteRecursively()
        runner.run(args)
        successMarker.writeText(successMarkerText(args))
    }

    private fun successMarkerText(args: List<String>): String {
        return args.joinToString("\n")
    }

    private fun logInfo(message: String) = runner.project.logger.info("${Logging.prefix}: $message")

    private fun logQuiet(message: String) = runner.project.logger.quiet("${Logging.prefix}: $message")

    private object Logging {
        const val prefix = "Native Distribution Commonization"
    }
}

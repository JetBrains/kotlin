/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.internal.impldep.org.junit.platform.commons.support.AnnotationSupport.findAnnotation
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.test.WithMuteInDatabase
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.streams.asStream

/**
 * Base class for all Kotlin Gradle plugin integration tests.
 */
@Tag("JUnit5")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WithMuteInDatabase
abstract class KGPBaseTest {
    open val defaultBuildOptions = BuildOptions()

    @TempDir
    lateinit var workingDir: Path

    data class BuildOptions(
        val logLevel: LogLevel = LogLevel.INFO,
        val kotlinVersion: String = TestVersions.Kotlin.CURRENT,
        val warningMode: WarningMode = WarningMode.Fail,
        val configurationCache: Boolean = false,
        val configurationCacheProblems: BaseGradleIT.ConfigurationCacheProblems = BaseGradleIT.ConfigurationCacheProblems.FAIL,
        val parallel: Boolean = true,
        val maxWorkers: Int = (Runtime.getRuntime().availableProcessors() / 4 - 1).coerceAtLeast(2),
        val fileSystemWatchEnabled: Boolean = false,
        val buildCacheEnabled: Boolean = false,
    ) {
        fun toArguments(
            gradleVersion: GradleVersion
        ): List<String> {
            val arguments = mutableListOf<String>()
            when (logLevel) {
                LogLevel.DEBUG -> arguments.add("--debug")
                LogLevel.INFO -> arguments.add("--info")
                LogLevel.WARN -> arguments.add("--warn")
                LogLevel.QUIET -> arguments.add("--quiet")
                else -> Unit
            }
            arguments.add("-Pkotlin_version=$kotlinVersion")
            when (warningMode) {
                WarningMode.Fail -> arguments.add("--warning-mode=fail")
                WarningMode.All -> arguments.add("--warning-mode=all")
                WarningMode.Summary -> arguments.add("--warning-mode=summary")
                WarningMode.None -> arguments.add("--warning-mode=none")
            }

            if (gradleVersion >= GradleVersion.version("6.6.0")) {
                arguments.add("-Dorg.gradle.unsafe.configuration-cache=$configurationCache")
                arguments.add("-Dorg.gradle.unsafe.configuration-cache-problems=${configurationCacheProblems.name.toLowerCase()}")
            }
            if (parallel) {
                arguments.add("--parallel")
                arguments.add("--max-workers=$maxWorkers")
            } else {
                arguments.add("--no-parallel")
            }

            if (gradleVersion >= GradleVersion.version("6.5")) {
                if (fileSystemWatchEnabled) {
                    arguments.add("--watch-fs")
                } else {
                    arguments.add("--no-watch-fs")
                }
            }

            arguments.add(if (buildCacheEnabled) "--build-cache" else "--no-build-cache")

            return arguments.toList()
        }
    }

    class GradleArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(
            context: ExtensionContext
        ): Stream<out Arguments> {
            val versionsAnnotation = findAnnotation(context.testMethod, GradleTestVersions::class.java).orElseThrow {
                IllegalStateException("Define allowed Gradle versions via '@GradleTestVersions'.")
            }

            val minGradleVersion = GradleVersion.version(versionsAnnotation.minVersion)
            val maxGradleVersion = GradleVersion.version(versionsAnnotation.maxVersion)
            val additionalGradleVersions = versionsAnnotation
                .additionalVersions
                .map(GradleVersion::version)
            additionalGradleVersions.forEach {
                assert(it in minGradleVersion..maxGradleVersion) {
                    "Additional Gradle version ${it.version} should be between ${minGradleVersion.version} and ${maxGradleVersion.version}"
                }
            }

            return sequenceOf(minGradleVersion, *additionalGradleVersions.toTypedArray(), maxGradleVersion)
                .map { Arguments.of(it) }
                .asStream()
        }
    }

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class GradleTestVersions(
        val minVersion: String = TestVersions.Gradle.MIN_SUPPORTED,
        val maxVersion: String = TestVersions.Gradle.MAX_SUPPORTED,
        val additionalVersions: Array<String> = []
    )
}
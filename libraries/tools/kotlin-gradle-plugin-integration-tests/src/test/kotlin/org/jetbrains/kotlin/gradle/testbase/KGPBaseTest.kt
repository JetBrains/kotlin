/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.internal.impldep.org.junit.platform.commons.support.AnnotationSupport.findAnnotation
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.utils.minSupportedGradleVersion
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.streams.asStream

/**
 * Base class for all Kotlin Gradle plugin integration tests.
 */
abstract class KGPBaseTest {
    open val defaultBuildOptions = BuildOptions()

    companion object {
        @TempDir
        @JvmStatic
        lateinit var workingDir: Path
    }

    data class BuildOptions(
        val logLevel: LogLevel = LogLevel.INFO,
        val kotlinVersion: String = KOTLIN_VERSION,
        val warningMode: WarningMode = WarningMode.Fail
    ) {
        fun toArguments(): List<String> {
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
        val minVersion: String = minSupportedGradleVersion,
        val maxVersion: String = "7.0-milestone-3",
        val additionalVersions: Array<String> = []
    )
}
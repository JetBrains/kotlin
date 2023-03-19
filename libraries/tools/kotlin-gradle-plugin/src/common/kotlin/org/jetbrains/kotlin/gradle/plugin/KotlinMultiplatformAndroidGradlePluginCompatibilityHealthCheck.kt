/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN
import org.jetbrains.kotlin.gradle.utils.androidPluginIds
import org.jetbrains.kotlin.gradle.utils.findAppliedAndroidPluginIdOrNull
import org.jetbrains.kotlin.gradle.utils.getOrPutRootProjectProperty
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheck
import java.util.concurrent.atomic.AtomicBoolean

internal object KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck {

    val compatibleAndroidGradlePluginVersionRange = AndroidGradlePluginVersionRange(
        minSupportedVersion = AndroidGradlePluginVersionRange.Version(4, 2),
        maxSupportedVersion = AndroidGradlePluginVersionRange.Version(8, 0)
    )

    /**
     * Used to store previously emitted messages in the build
     */
    private const val PROPERTY_KEY_EMITTED_MESSAGES = "KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.emittedMessages"

    /**
     * Used to store project paths that executed this health check
     */
    const val PROPERTY_KEY_EXECUTED_PROJECT_PATHS = "KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.executedProjects"

    data class AndroidGradlePluginVersionRange(
        val minSupportedVersion: Version,
        val maxSupportedVersion: Version
    ) {

        constructor(
            minSupportedVersionMajor: Int, minSupportedVersionMinor: Int = 0,
            maxSupportedVersionMajor: Int, maxSupportedVersionMinor: Int = 0
        ) : this(
            minSupportedVersion = Version(minSupportedVersionMajor, minSupportedVersionMinor),
            maxSupportedVersion = Version(maxSupportedVersionMajor, maxSupportedVersionMinor)
        )

        data class Version(val major: Int, val minor: Int)

        fun isTooHigh(version: AndroidGradlePluginVersion): Boolean {
            if (version.major > this.maxSupportedVersion.major) return true
            if (version.major < this.maxSupportedVersion.major) return false
            return version.minor > this.maxSupportedVersion.minor
        }

        fun isTooLow(version: AndroidGradlePluginVersion): Boolean {
            if (version.major < this.minSupportedVersion.major) return true
            if (version.major > this.minSupportedVersion.major) return false
            return version.minor < this.minSupportedVersion.minor
        }
    }

    interface AndroidGradlePluginVersionProvider {
        fun getAndroidGradlePluginVersion(): AndroidGradlePluginVersion?

        object Default : AndroidGradlePluginVersionProvider {
            override fun getAndroidGradlePluginVersion(): AndroidGradlePluginVersion? {
                return AndroidGradlePluginVersion.currentOrNull
            }
        }
    }

    fun Project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied(
        warningLogger: (warningMessage: String) -> Unit = project.logger::warn,
        androidGradlePluginVersionProvider: AndroidGradlePluginVersionProvider = AndroidGradlePluginVersionProvider.Default
    ) {
        val emittedMessages = project.getOrPutRootProjectProperty(PROPERTY_KEY_EMITTED_MESSAGES) { mutableSetOf<String>() }

        val deduplicateWarningMessageLogger = { message: String ->
            if (emittedMessages.add(message)) {
                warningLogger(message)
            }
        }

        val executed = AtomicBoolean(false)
        androidPluginIds.forEach { id ->
            plugins.withId(id) {
                if (!executed.getAndSet(true)) {
                    runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
                        deduplicateWarningMessageLogger, androidGradlePluginVersionProvider
                    )
                }
            }
        }
    }

    fun Project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
        warningLogger: (warningMessage: String) -> Unit = project.logger::warn,
        androidGradlePluginVersionProvider: AndroidGradlePluginVersionProvider = AndroidGradlePluginVersionProvider.Default,
        compatibleAndroidGradlePluginVersionRange: AndroidGradlePluginVersionRange =
            KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.compatibleAndroidGradlePluginVersionRange
    ) = runProjectConfigurationHealthCheck check@{
        if (project.kotlinPropertiesProvider.ignoreAndroidGradlePluginCompatibilityIssues) return@check
        getOrPutRootProjectProperty(PROPERTY_KEY_EXECUTED_PROJECT_PATHS) { mutableSetOf<String>() }.add(path)

        /* Return when no android plugin is applied */
        findAppliedAndroidPluginIdOrNull() ?: return@check

        val androidGradlePluginVersion = androidGradlePluginVersionProvider.getAndroidGradlePluginVersion()
            ?: return warningLogger(Messages.failedGettingAndroidGradlePluginVersion())

        if (compatibleAndroidGradlePluginVersionRange.isTooLow(androidGradlePluginVersion)) {
            warningLogger(Messages.androidGradlePluginVersionTooLow(androidGradlePluginVersion.toString()))
        }

        if (compatibleAndroidGradlePluginVersionRange.isTooHigh(androidGradlePluginVersion)) {
            warningLogger(Messages.androidGradlePluginVersionTooHigh(androidGradlePluginVersion.toString()))
        }
    }

    object Messages {

        fun failedGettingAndroidGradlePluginVersion() =
            "w: Failed to get AndroidGradlePluginVersion"

        fun androidGradlePluginVersionTooLow(androidGradlePluginVersionString: String) = createCompatibilityWarningMessage(
            "The applied Android Gradle Plugin version ($androidGradlePluginVersionString) is lower than the minimum supported"
        )

        fun androidGradlePluginVersionTooHigh(androidGradlePluginVersionString: String) = createCompatibilityWarningMessage(
            "The applied Android Gradle Plugin version ($androidGradlePluginVersionString) " +
                    "is higher than the maximum known to the Kotlin Gradle Plugin. " +
                    "Tooling stability in such configuration isn't tested, please report encountered issues to https://kotl.in/issue"
        )

        private fun createCompatibilityWarningMessage(warning: String) = buildString {
            appendLine("w: Kotlin Multiplatform <-> Android Gradle Plugin compatibility issue: $warning")
            appendLine(
                "Minimum supported Android Gradle Plugin version: " +
                        "${compatibleAndroidGradlePluginVersionRange.minSupportedVersion.major}." +
                        "${compatibleAndroidGradlePluginVersionRange.minSupportedVersion.minor}"
            )
            appendLine(
                "Maximum tested Android Gradle Plugin version: " +
                        "${compatibleAndroidGradlePluginVersionRange.maxSupportedVersion.major}." +
                        "${compatibleAndroidGradlePluginVersionRange.maxSupportedVersion.minor}"
            )
            appendLine("To suppress this message add '$KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN=true' to your gradle.properties")
        }
    }
}

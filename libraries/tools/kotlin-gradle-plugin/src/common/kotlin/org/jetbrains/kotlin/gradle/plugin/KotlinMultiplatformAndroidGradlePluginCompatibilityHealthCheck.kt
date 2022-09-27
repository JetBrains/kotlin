/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import com.android.Version
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
        minSupportedVersion = AndroidGradlePluginVersion(4, 1),
        maxSupportedVersion = AndroidGradlePluginVersion(7, 3)
    )

    /**
     * Used to store previously emitted messages in the build
     */
    private const val PROPERTY_KEY_EMITTED_MESSAGES = "KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.emittedMessages"

    /**
     * Used to store project paths that executed this health check
     */
    const val PROPERTY_KEY_EXECUTED_PROJECT_PATHS = "KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.executedProjects"

    data class AndroidGradlePluginVersion(val major: Int, val minor: Int) : Comparable<AndroidGradlePluginVersion> {
        override fun compareTo(other: AndroidGradlePluginVersion): Int {
            if (this.major != other.major) return this.major.compareTo(other.major)
            return this.minor.compareTo(other.minor)
        }

        override fun toString(): String {
            return "$major.$minor"
        }
    }

    data class AndroidGradlePluginVersionRange(
        val minSupportedVersion: AndroidGradlePluginVersion,
        val maxSupportedVersionMajor: Int,
        val maxSupportedVersionMinor: Int?
    ) {
        constructor(
            minSupportedVersionMajor: Int,
            minSupportedVersionMinor: Int,
            maxSupportedVersionMajor: Int,
            maxSupportedVersionMinor: Int? = null
        ) : this(
            minSupportedVersion = AndroidGradlePluginVersion(minSupportedVersionMajor, minSupportedVersionMinor),
            maxSupportedVersionMajor = maxSupportedVersionMajor,
            maxSupportedVersionMinor = maxSupportedVersionMinor
        )

        constructor(
            minSupportedVersion: AndroidGradlePluginVersion,
            maxSupportedVersion: AndroidGradlePluginVersion
        ) : this(
            minSupportedVersion = minSupportedVersion,
            maxSupportedVersionMajor = maxSupportedVersion.major,
            maxSupportedVersionMinor = maxSupportedVersion.minor
        )

        fun isTooHigh(version: AndroidGradlePluginVersion): Boolean {
            return if (maxSupportedVersionMinor != null)
                version > AndroidGradlePluginVersion(maxSupportedVersionMajor, maxSupportedVersionMinor)
            else version.major > maxSupportedVersionMajor
        }

        fun isTooLow(version: AndroidGradlePluginVersion): Boolean {
            return version < minSupportedVersion
        }
    }

    interface AndroidGradlePluginStringProvider {
        fun getAndroidGradlePluginString(): String?

        object Default : AndroidGradlePluginStringProvider {
            override fun getAndroidGradlePluginString(): String? =
                runCatching { Version.ANDROID_GRADLE_PLUGIN_VERSION }.getOrElse { return null }
        }
    }

    interface AndroidGradlePluginVersionParser {
        fun parseVersionString(version: String): AndroidGradlePluginVersion?

        object Default : AndroidGradlePluginVersionParser {
            private val versionRegex = Regex("""(\d+)\.(\d+)(\.\d+)?(.*)""")

            override fun parseVersionString(version: String): AndroidGradlePluginVersion? {
                val matchResult = versionRegex.matchEntire(version) ?: return null
                val major = matchResult.groups[1]?.value?.toIntOrNull() ?: return null
                val minor = matchResult.groups[2]?.value?.toIntOrNull() ?: return null
                return AndroidGradlePluginVersion(major, minor)
            }
        }
    }

    fun Project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied(
        warningLogger: (warningMessage: String) -> Unit = project.logger::warn,
        androidGradlePluginStringProvider: AndroidGradlePluginStringProvider = AndroidGradlePluginStringProvider.Default,
        androidGradlePluginVersionParser: AndroidGradlePluginVersionParser = AndroidGradlePluginVersionParser.Default
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
                        deduplicateWarningMessageLogger, androidGradlePluginStringProvider, androidGradlePluginVersionParser
                    )
                }
            }
        }
    }

    fun Project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheck(
        warningLogger: (warningMessage: String) -> Unit = project.logger::warn,
        androidGradlePluginStringProvider: AndroidGradlePluginStringProvider = AndroidGradlePluginStringProvider.Default,
        androidGradlePluginVersionParser: AndroidGradlePluginVersionParser = AndroidGradlePluginVersionParser.Default,
        compatibleAndroidGradlePluginVersionRange: AndroidGradlePluginVersionRange =
            KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.compatibleAndroidGradlePluginVersionRange
    ) = runProjectConfigurationHealthCheck check@{
        if (project.kotlinPropertiesProvider.ignoreAndroidGradlePluginCompatibilityIssues) return@check
        getOrPutRootProjectProperty(PROPERTY_KEY_EXECUTED_PROJECT_PATHS) { mutableSetOf<String>() }.add(path)

        /* Return when no android plugin is applied */
        findAppliedAndroidPluginIdOrNull() ?: return@check

        val androidGradlePluginVersionString = androidGradlePluginStringProvider.getAndroidGradlePluginString()
            ?: return warningLogger(Messages.FAILED_GETTING_ANDROID_GRADLE_PLUGIN_VERSION_STRING)

        val androidGradlePluginVersion = androidGradlePluginVersionParser.parseVersionString(androidGradlePluginVersionString)
            ?: return warningLogger(Messages.failedParsingAndroidGradlePluginVersion(androidGradlePluginVersionString))

        if (compatibleAndroidGradlePluginVersionRange.isTooLow(androidGradlePluginVersion)) {
            warningLogger(Messages.androidGradlePluginVersionTooLow(androidGradlePluginVersionString))
        }

        if (compatibleAndroidGradlePluginVersionRange.isTooHigh(androidGradlePluginVersion)) {
            warningLogger(Messages.androidGradlePluginVersionTooHigh(androidGradlePluginVersionString))
        }
    }

    object Messages {
        const val FAILED_GETTING_ANDROID_GRADLE_PLUGIN_VERSION_STRING = "Failed to get the version of the applied Android Gradle Plugin"

        fun failedParsingAndroidGradlePluginVersion(androidGradlePluginVersionString: String) =
            "w: Failed to parse the version of the applied Android Gradle Plugin: $androidGradlePluginVersionString"

        fun androidGradlePluginVersionTooLow(androidGradlePluginVersionString: String) = createCompatibilityWarningMessage(
            "The applied Android Gradle Plugin version ($androidGradlePluginVersionString) is lower than the minimum supported"
        )

        fun androidGradlePluginVersionTooHigh(androidGradlePluginVersionString: String) = createCompatibilityWarningMessage(
            "The applied Android Gradle Plugin version ($androidGradlePluginVersionString) " +
                    "is higher than the maximum known to the Kotlin Gradle Plugin. " +
                    "Tooling stability in such configuration isn't tested, please report encountered issues to kotl.in/issue"
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
                        "${compatibleAndroidGradlePluginVersionRange.maxSupportedVersionMajor}." +
                        "${compatibleAndroidGradlePluginVersionRange.maxSupportedVersionMinor ?: "*"}"
            )
            appendLine("To suppress this message add $KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN=true to your gradle.properties")
        }
    }
}

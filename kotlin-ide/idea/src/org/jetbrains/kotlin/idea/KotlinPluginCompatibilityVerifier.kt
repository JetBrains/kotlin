/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.ui.Messages
import com.intellij.util.PlatformUtils
import com.intellij.util.text.nullize

object KotlinPluginCompatibilityVerifier {
    @JvmStatic
    fun checkCompatibility() {
        val kotlinVersion = KotlinPluginVersion.getCurrent() ?: return
        val platformVersion = PlatformVersion.getCurrent() ?: return

        if (kotlinVersion.platformVersion.platform != platformVersion.platform) {
            Messages.showWarningDialog(
                KotlinBundle.message("plugin.verifier.compatibility.issue.message", kotlinVersion, platformVersion),
                KotlinBundle.message("plugin.verifier.compatibility.issue.title")
            )
        }
    }
}

interface KotlinPluginVersion {
    val platformVersion: PlatformVersion
    val buildNumber: String?

    companion object {
        fun parse(version: String): KotlinPluginVersion? {
            return OldKotlinPluginVersion.parse(version) ?: KidKotlinPluginVersion.parse(version)
        }

        fun getCurrent(): KotlinPluginVersion? = parse(KotlinPluginUtil.getPluginVersion())
    }
}

data class KidKotlinPluginVersion(
    override val buildNumber: String?, // 53
    override val platformVersion: PlatformVersion,
) : KotlinPluginVersion {
    companion object {
        private val KID_KOTLIN_VERSION_REGEX = "([\\d]{3}).([\\d]+)-kid".toRegex()

        fun parse(version: String): KidKotlinPluginVersion? {
            val matchResult = KID_KOTLIN_VERSION_REGEX.matchEntire(version) ?: return null
            val (platformNumber, buildNumber) = matchResult.destructured
            val platformVersionText = "20" + platformNumber.take(2) + "." + platformNumber.takeLast(1)
            val platformVersion = PlatformVersion(PlatformVersion.Platform.IDEA, platformVersionText)
            return KidKotlinPluginVersion(buildNumber, platformVersion)
        }
    }
}

data class OldKotlinPluginVersion(
    val kotlinVersion: String, // 1.2.3
    val milestone: String?, // M1
    val status: String, // release, eap, rc
    override val buildNumber: String?, // 53
    override val platformVersion: PlatformVersion,
    val patchNumber: String // usually '1'
) : KotlinPluginVersion {
    companion object {
        private const val KOTLIN_VERSION_REGEX_STRING =
            "^([\\d.]+)" +                // Version number, like 1.3.50
                    "(?:-(M\\d+))?" +     // (Optional) M-release, like M2
                    "-([A-Za-z]+)" +      // status, like 'eap/dev/release'
                    "(?:-(\\d+))?" +      // (Optional) buildNumber (absent for 'release')
                    "-([A-Za-z0-9.]+)" +  // Platform version, like Studio4.0.1
                    "-(\\d+)$"            // Tooling update, like '-1'

        private val OLD_KOTLIN_VERSION_REGEX = KOTLIN_VERSION_REGEX_STRING.toRegex()

        fun parse(version: String): OldKotlinPluginVersion? {
            val matchResult = OLD_KOTLIN_VERSION_REGEX.matchEntire(version) ?: return null
            val (kotlinVersion, milestone, status, buildNumber, platformString, patchNumber) = matchResult.destructured
            val platformVersion = PlatformVersion.parse(platformString) ?: return null
            return OldKotlinPluginVersion(
                kotlinVersion,
                milestone.nullize(),
                status,
                buildNumber.nullize(),
                platformVersion,
                patchNumber
            )
        }
    }

    override fun toString() = "$kotlinVersion for $platformVersion"
}

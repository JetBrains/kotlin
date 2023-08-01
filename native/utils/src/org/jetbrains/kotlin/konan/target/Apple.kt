/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed -> in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.properties.KonanPropertiesLoader
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.util.InternalServer
import kotlin.math.max

class AppleConfigurablesImpl(
        target: KonanTarget,
        properties: Properties,
        baseDir: String?
) : AppleConfigurables, KonanPropertiesLoader(target, properties, baseDir) {

    private val sdkDependency = this.targetSysRoot!!
    private val toolchainDependency = this.targetToolchain!!
    private val xcodeAddonDependency = this.additionalToolsDir!!

    override val absoluteTargetSysRoot: String get() = when (val provider = xcodePartsProvider) {
        is XcodePartsProvider.Local -> provider.xcode.pathToPlatformSdk(platformName())
        XcodePartsProvider.InternalServer -> absolute(sdkDependency)
    }

    override val osVersionMin: String by lazy {
        // A hack only for 1.9.10 where we need to keep the old defaults for Xcode 14 (min version 9.0)
        // and the new ones for Xcode 15 (min version 12.0).
        if (target.family == Family.IOS || target.family == Family.TVOS) {
            when (val xcodePartsProvider1 = xcodePartsProvider) {
                is XcodePartsProvider.Local -> {
                    if (checkXcodeVersion("15.0.0", xcodePartsProvider1.xcode.version)) {
                        return@lazy targetString("osVersionMinSinceXcode15")!!
                    }
                }
                is XcodePartsProvider.InternalServer -> {
                    // Build server case. Here we use Xcode 14, so we don't need a workaround here.
                }
            }
        }
        super.osVersionMin
    }

    override val absoluteTargetToolchain: String get() = when (val provider = xcodePartsProvider) {
        is XcodePartsProvider.Local -> provider.xcode.toolchain
        XcodePartsProvider.InternalServer -> absolute(toolchainDependency)
    }

    override val absoluteAdditionalToolsDir: String get() = when (val provider = xcodePartsProvider) {
        is XcodePartsProvider.Local -> provider.xcode.additionalTools
        XcodePartsProvider.InternalServer -> absolute(additionalToolsDir)
    }

    override val dependencies get() = super.dependencies + when (xcodePartsProvider) {
        is XcodePartsProvider.Local -> emptyList()
        XcodePartsProvider.InternalServer -> listOf(sdkDependency, toolchainDependency, xcodeAddonDependency)
    }

    private val xcodePartsProvider by lazy {
        if (InternalServer.isAvailable) {
            XcodePartsProvider.InternalServer
        } else {
            val xcode = Xcode.findCurrent()

            if (properties.getProperty("ignoreXcodeVersionCheck") != "true") {
                properties.getProperty("minimalXcodeVersion")?.let { minimalXcodeVersion ->
                    val currentXcodeVersion = xcode.version
                    if (!checkXcodeVersion(minimalXcodeVersion, currentXcodeVersion)) {
                        error("Unsupported Xcode version $currentXcodeVersion, minimal supported version is $minimalXcodeVersion.")
                    }
                }
            }

            XcodePartsProvider.Local(xcode)
        }
    }

    /**
     * Checks if the current Xcode version meets the minimal version requirement.
     *
     * @param minimalVersion The minimal Xcode version to check against.
     * @param currentVersion The current Xcode version.
     * @return true if the current Xcode version is greater than or equal to the minimal version,
     *         false otherwise.
     */
    private fun checkXcodeVersion(minimalVersion: String, currentVersion: String): Boolean {
        // Xcode versions contain only numbers (even betas).
        // But we still split by '-' and whitespaces to take into account versions like 11.2-beta.
        val minimalVersionParts = minimalVersion.split("(\\s+|\\.|-)".toRegex()).map { it.toIntOrNull() ?: 0 }
        val currentVersionParts = currentVersion.split("(\\s+|\\.|-)".toRegex()).map { it.toIntOrNull() ?: 0 }
        val size = max(minimalVersionParts.size, currentVersionParts.size)

        for (i in 0 until size) {
            val currentPart = currentVersionParts.getOrElse(i) { 0 }
            val minimalPart = minimalVersionParts.getOrElse(i) { 0 }

            when {
                currentPart > minimalPart -> return true
                currentPart < minimalPart -> return false
            }
        }
        return true
    }

    private sealed class XcodePartsProvider {
        class Local(val xcode: Xcode) : XcodePartsProvider()
        object InternalServer : XcodePartsProvider()
    }
}

/**
 * Name of an Apple platform as in Xcode.app/Contents/Developer/Platforms.
 */
fun AppleConfigurables.platformName(): String = when (target.family) {
    Family.OSX -> "MacOSX"
    Family.IOS -> if (targetTriple.isSimulator) {
        "iPhoneSimulator"
    } else {
        "iPhoneOS"
    }
    Family.TVOS -> if (targetTriple.isSimulator) {
        "AppleTVSimulator"
    } else {
        "AppleTVOS"
    }
    Family.WATCHOS -> if (targetTriple.isSimulator) {
        "WatchSimulator"
    } else {
        "WatchOS"
    }
    else -> error("Not an Apple target: $target")
}

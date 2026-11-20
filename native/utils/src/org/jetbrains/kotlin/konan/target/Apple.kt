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
import org.jetbrains.kotlin.konan.util.InternalServer
import org.jetbrains.kotlin.konan.util.ProgressCallback
import java.util.Properties
import java.io.File

class AppleConfigurablesImpl(
    target: KonanTarget,
    properties: Properties,
    dependenciesDir: String?,
    progressCallback: ProgressCallback,
) : AppleConfigurables, KonanPropertiesLoader(target, properties, dependenciesDir, progressCallback = progressCallback) {

    override val absoluteTargetSysRoot: String
        get() {
            // In the case of Mac Catalyst, we use sysroot from macOS.
            val platformName = if (targetTriple.isMacabi) "MacOSX" else platformName()
            return xcode.pathToPlatformSdk(platformName)
        }

    override val absoluteTargetToolchain: String get() = xcode.toolchain

    override val absoluteAdditionalToolsDir: String get() = xcode.additionalTools

    // Opt-in (set at build time via -Xoverride-konan-properties=useProvisionedXcode=true): resolve the Apple
    // toolchain/sysroot from a whole Xcode provisioned under $dependenciesRoot/xcode_<version>_<build>.
    private val useProvisionedXcode: Boolean
        get() = properties.getProperty("useProvisionedXcode") == "true"

    val xcode: Xcode by lazy {
        if (useProvisionedXcode) provisionedXcode()
        else currentXcodeCheckingVersion()
    }

    // The whole Xcode selected by useProvisionedXcode: a [CurrentXcode] rooted at the provisioned Xcode.app under
    // $dependenciesRoot/xcode_<version>_<build> (via DEVELOPER_DIR), so its toolchain/SDKs are queried from that
    // specific Xcode rather than the system selection.
    private fun provisionedXcode(): Xcode {
        val version = properties.getProperty("xcodeVersion")
            ?: error("useProvisionedXcode is set but 'xcodeVersion' is missing from konan.properties")
        val build = properties.getProperty("xcodeBuild")
            ?: error("useProvisionedXcode is set but 'xcodeBuild' is missing from konan.properties")
        val root = dependenciesRoot
            ?: error("useProvisionedXcode is set but the dependencies root is not configured")
        val xcodeApp = File(root).resolve("xcode_${version}_${build}")
        // If the build side has already snapshotted the provisioned Xcode into Xcode.xcodeOverride (done inside a
        // Gradle ValueSource, so configuration-cache-safe), reuse it rather than resolving the toolchain/SDKs live
        // via xcrun — which at configuration time would break the configuration cache. In the compiler process the
        // override is unset, so we resolve the provisioned Xcode directly (xcrun at execution time is fine).
        return Xcode.xcodeOverride ?: InstalledXcode(developerDir = xcodeApp.resolve("Contents/Developer").path)
    }

    private fun currentXcodeCheckingVersion(): Xcode {
        val xcode = Xcode.findCurrent()
        if (properties.getProperty("ignoreXcodeVersionCheck") != "true") {
            properties.getProperty("minimalXcodeVersion")?.let(XcodeVersion::parse)?.let { minimalXcodeVersion ->
                checkXcodeVersion(minimalXcodeVersion, xcode.version)
            }
        }
        return xcode
    }

    private fun checkXcodeVersion(minimalVersion: XcodeVersion, currentVersion: XcodeVersion) {
        if (currentVersion < minimalVersion) {
            error("Unsupported Xcode version $currentVersion, minimal supported version is $minimalVersion.")
        }
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

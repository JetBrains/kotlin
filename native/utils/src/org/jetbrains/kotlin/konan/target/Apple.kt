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

    private val sdkDependency = this.targetSysRoot!!
    private val toolchainDependency = this.targetToolchain!!
    private val xcodeAddonDependency = this.additionalToolsDir // Only macOS hosts have one.

    override val absoluteTargetSysRoot: String get() = when (val provider = xcodePartsProvider) {
        is XcodePartsProvider.Local -> {
            // In the case of Mac Catalyst, we use sysroot from macOS.
            val platformName = if (targetTriple.isMacabi) "MacOSX" else platformName()
            provider.xcode.pathToPlatformSdk(platformName)
        }
        XcodePartsProvider.InternalServer -> absolute(sdkDependency)
        XcodePartsProvider.Configured -> absoluteConfigured(sdkDependency, "targetSysRoot.${target.name}")
    }

    override val absoluteTargetToolchain: String get() = when (val provider = xcodePartsProvider) {
        is XcodePartsProvider.Local -> provider.xcode.toolchain
        XcodePartsProvider.InternalServer -> "${absolute(toolchainDependency)}/usr"
        XcodePartsProvider.Configured ->
            "${absoluteConfigured(toolchainDependency, "targetToolchain.${hostTargetSuffix(HostManager.host, target)}")}/usr"
    }

    override val absoluteAdditionalToolsDir: String get() = when (val provider = xcodePartsProvider) {
        is XcodePartsProvider.Local -> provider.xcode.additionalTools
        XcodePartsProvider.InternalServer -> absolute(xcodeAddonDependency)
        XcodePartsProvider.Configured -> error("Additional Xcode tools are not available for $target on a ${HostManager.hostName} host")
    }

    override val dependencies
        get() = super.dependencies +
                if (HostManager.hostIsMac && InternalServer.isAvailable && !useProvisionedXcode) listOfNotNull(
                    sdkDependency,
                    toolchainDependency,
                    xcodeAddonDependency
                ) else emptyList()

    // Opt-in (set at build time via -Xoverride-konan-properties=useProvisionedXcode=true): resolve the Apple
    // toolchain/sysroot from a whole Xcode provisioned under $dependenciesRoot/xcode_<version>_<build>.
    private val useProvisionedXcode: Boolean
        get() = properties.getProperty("useProvisionedXcode") == "true"

    private val xcodePartsProvider by lazy {
        when {
            !HostManager.hostIsMac -> XcodePartsProvider.Configured
            useProvisionedXcode -> XcodePartsProvider.Local(provisionedXcode())
            InternalServer.isAvailable -> XcodePartsProvider.InternalServer
            else -> XcodePartsProvider.Local(currentXcodeCheckingVersion())
        }
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

    // A path from the properties, which on a host without Xcode is either absolute or inside a declared dependency.
    // The defaults name artifacts of the internal server, which are only declared as dependencies on macOS hosts.
    private fun absoluteConfigured(path: String, property: String): String {
        check(File(path).isAbsolute || File(path).toPath().first().toString() in dependencies) {
            "On a ${HostManager.hostName} host, $property must be an absolute path or inside a declared dependency, but it is '$path'"
        }
        return absolute(path)
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

    private sealed class XcodePartsProvider {
        class Local(val xcode: Xcode) : XcodePartsProvider()
        object InternalServer : XcodePartsProvider()

        /**
         * A host other than macOS, which has no Xcode: the toolchain and the SDK are the ones named by
         * `targetToolchain.<host>-<target>` and `targetSysRoot.<target>`, laid out as in Xcode.
         * Only caches can be built with them; see [PlatformManager.cacheOnlyTargets].
         */
        object Configured : XcodePartsProvider()
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

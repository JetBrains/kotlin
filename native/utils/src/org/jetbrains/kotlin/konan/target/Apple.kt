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

class AppleConfigurablesImpl(
    target: KonanTarget,
    properties: Properties,
    dependenciesDir: String?
) : AppleConfigurables, KonanPropertiesLoader(target, properties, dependenciesDir) {

    private val sdkDependency = this.targetSysRoot!!
    private val toolchainDependency = this.targetToolchain!!
    private val xcodeAddonDependency = this.additionalToolsDir!!

    override val absoluteTargetSysRoot: String get() = when (val provider = xcodePartsProvider) {
        is XcodePartsProvider.Local -> provider.xcode.pathToPlatformSdk(platformName())
        XcodePartsProvider.InternalServer -> absolute(sdkDependency)
    }

    override val absoluteTargetToolchain: String get() = when (val provider = xcodePartsProvider) {
        is XcodePartsProvider.Local -> provider.xcode.toolchain
        XcodePartsProvider.InternalServer -> "${absolute(toolchainDependency)}/usr"
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
                properties.getProperty("minimalXcodeVersion")?.let(XcodeVersion::parse)?.let { minimalXcodeVersion ->
                    val currentXcodeVersion = xcode.version
                    checkXcodeVersion(minimalXcodeVersion, currentXcodeVersion)
                }
            }

            XcodePartsProvider.Local(xcode)
        }
    }

    private fun checkXcodeVersion(minimalVersion: XcodeVersion, currentVersion: XcodeVersion) {
        if (currentVersion < minimalVersion) {
            error("Unsupported Xcode version $currentVersion, minimal supported version is $minimalVersion.")
        }
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

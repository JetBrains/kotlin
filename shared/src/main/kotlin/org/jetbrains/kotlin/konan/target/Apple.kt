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
        baseDir: String?
) : AppleConfigurables, KonanPropertiesLoader(target, properties, baseDir) {

    private val sdkDependency = this.targetSysRoot!!
    private val toolchainDependency = this.targetToolchain!!
    private val xcodeAddonDependency = this.additionalToolsDir!!

    override val absoluteTargetSysRoot: String get() = when (xcodePartsProvider) {
        is XcodePartsProvider.Local -> when (target) {
            KonanTarget.MACOS_X64 -> xcodePartsProvider.xcode.macosxSdk
            KonanTarget.IOS_ARM32, KonanTarget.IOS_ARM64 -> xcodePartsProvider.xcode.iphoneosSdk
            KonanTarget.IOS_X64 -> xcodePartsProvider.xcode.iphonesimulatorSdk
            KonanTarget.TVOS_ARM64 -> xcodePartsProvider.xcode.appletvosSdk
            KonanTarget.TVOS_X64 -> xcodePartsProvider.xcode.appletvsimulatorSdk
            KonanTarget.WATCHOS_ARM64, KonanTarget.WATCHOS_ARM32 -> xcodePartsProvider.xcode.watchosSdk
            KonanTarget.WATCHOS_X64, KonanTarget.WATCHOS_X86 -> xcodePartsProvider.xcode.watchsimulatorSdk
            else -> error(target)
        }
        XcodePartsProvider.InternalServer -> absolute(sdkDependency)
    }

    override val absoluteTargetToolchain: String get() = when (xcodePartsProvider) {
        is XcodePartsProvider.Local -> xcodePartsProvider.xcode.toolchain
        XcodePartsProvider.InternalServer -> absolute(toolchainDependency)
    }

    override val absoluteAdditionalToolsDir: String get() = when (xcodePartsProvider) {
        is XcodePartsProvider.Local -> xcodePartsProvider.xcode.additionalTools
        XcodePartsProvider.InternalServer -> absolute(additionalToolsDir)
    }

    override val dependencies get() = super.dependencies + when (xcodePartsProvider) {
        is XcodePartsProvider.Local -> emptyList()
        XcodePartsProvider.InternalServer -> listOf(sdkDependency, toolchainDependency, xcodeAddonDependency)
    }

    private val xcodePartsProvider = if (InternalServer.isAvailable) {
        XcodePartsProvider.InternalServer
    } else {
        val xcode = Xcode.current
        properties.getProperty("useFixedXcodeVersion")?.let { requiredXcodeVersion ->
            val currentXcodeVersion = xcode.version

            if (properties.getProperty("ignoreXcodeVersionCheck") != "true" &&
                    currentXcodeVersion != requiredXcodeVersion) {
                error("expected Xcode version $requiredXcodeVersion, got $currentXcodeVersion, consider updating " +
                        "Xcode or use \"ignoreXcodeVersionCheck\" variable in konan.properties")
            }
        }

        XcodePartsProvider.Local(xcode)
    }

    private sealed class XcodePartsProvider {
        class Local(val xcode: Xcode) : XcodePartsProvider()
        object InternalServer : XcodePartsProvider()
    }
}

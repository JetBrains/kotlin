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
        is XcodePartsProvider.Local -> when (target) {
            KonanTarget.MACOS_X64, KonanTarget.MACOS_ARM64 -> provider.xcode.macosxSdk
            KonanTarget.IOS_ARM32, KonanTarget.IOS_ARM64 -> provider.xcode.iphoneosSdk
            KonanTarget.IOS_X64 -> provider.xcode.iphonesimulatorSdk
            KonanTarget.TVOS_ARM64 -> provider.xcode.appletvosSdk
            KonanTarget.TVOS_X64 -> provider.xcode.appletvsimulatorSdk
            KonanTarget.WATCHOS_ARM64, KonanTarget.WATCHOS_ARM32 -> provider.xcode.watchosSdk
            KonanTarget.WATCHOS_X64, KonanTarget.WATCHOS_X86 -> provider.xcode.watchsimulatorSdk
            else -> error(target)
        }
        XcodePartsProvider.InternalServer -> absolute(sdkDependency)
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
            val xcode = Xcode.current

            if (properties.getProperty("ignoreXcodeVersionCheck") != "true") {
                properties.getProperty("minimalXcodeVersion")?.let { minimalXcodeVersion ->
                    val currentXcodeVersion = xcode.version
                    checkXcodeVersion(minimalXcodeVersion, currentXcodeVersion)
                }
            }

            XcodePartsProvider.Local(xcode)
        }
    }

    private fun checkXcodeVersion(minimalVersion: String, currentVersion: String) {
        // Xcode versions contain only numbers (even betas).
        // But we still split by '-' and whitespaces to take into account versions like 11.2-beta.
        val minimalVersionParts = minimalVersion.split("(\\s+|\\.|-)".toRegex()).map { it.toIntOrNull() ?: 0 }
        val currentVersionParts = currentVersion.split("(\\s+|\\.|-)".toRegex()).map { it.toIntOrNull() ?: 0 }
        val size = max(minimalVersionParts.size, currentVersionParts.size)

        for (i in 0 until size) {
            val currentPart = currentVersionParts.getOrElse(i) { 0 }
            val minimalPart = minimalVersionParts.getOrElse(i) { 0 }

            when {
                currentPart > minimalPart -> return
                currentPart < minimalPart ->
                    error("Unsupported Xcode version $currentVersion, minimal supported version is $minimalVersion.")
            }
        }
    }

    private sealed class XcodePartsProvider {
        class Local(val xcode: Xcode) : XcodePartsProvider()
        object InternalServer : XcodePartsProvider()
    }
}

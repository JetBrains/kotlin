/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.util.Named
import java.io.Serializable

private const val DEPRECATION_LINK = "https://kotl.in/native-targets-tiers"
const val DEPRECATED_TARGET_MESSAGE = "Target is no longer available. See: $DEPRECATION_LINK"

@Suppress("ClassName")
sealed class KonanTarget(override val name: String, val family: Family, val architecture: Architecture) : Named, Serializable {
    object ANDROID_X64 : KonanTarget("android_x64", Family.ANDROID, Architecture.X64)
    object ANDROID_X86 : KonanTarget("android_x86", Family.ANDROID, Architecture.X86)
    object ANDROID_ARM32 : KonanTarget("android_arm32", Family.ANDROID, Architecture.ARM32)
    object ANDROID_ARM64 : KonanTarget("android_arm64", Family.ANDROID, Architecture.ARM64)
    object IOS_ARM64 : KonanTarget("ios_arm64", Family.IOS, Architecture.ARM64)
    object IOS_X64 : KonanTarget("ios_x64", Family.IOS, Architecture.X64)
    object IOS_SIMULATOR_ARM64 : KonanTarget("ios_simulator_arm64", Family.IOS, Architecture.ARM64)
    object WATCHOS_ARM32 : KonanTarget("watchos_arm32", Family.WATCHOS, Architecture.ARM32)
    object WATCHOS_ARM64 : KonanTarget("watchos_arm64", Family.WATCHOS, Architecture.ARM64)
    object WATCHOS_X64 : KonanTarget("watchos_x64", Family.WATCHOS, Architecture.X64)
    object WATCHOS_SIMULATOR_ARM64 : KonanTarget("watchos_simulator_arm64", Family.WATCHOS, Architecture.ARM64)
    object WATCHOS_DEVICE_ARM64 : KonanTarget("watchos_device_arm64", Family.WATCHOS, Architecture.ARM64)
    object TVOS_ARM64 : KonanTarget("tvos_arm64", Family.TVOS, Architecture.ARM64)
    object TVOS_X64 : KonanTarget("tvos_x64", Family.TVOS, Architecture.X64)
    object TVOS_SIMULATOR_ARM64 : KonanTarget("tvos_simulator_arm64", Family.TVOS, Architecture.ARM64)
    object LINUX_X64 : KonanTarget("linux_x64", Family.LINUX, Architecture.X64)
    object MINGW_X64 : KonanTarget("mingw_x64", Family.MINGW, Architecture.X64)
    object MACOS_X64 : KonanTarget("macos_x64", Family.OSX, Architecture.X64)
    object MACOS_ARM64 : KonanTarget("macos_arm64", Family.OSX, Architecture.ARM64)
    object LINUX_ARM64 : KonanTarget("linux_arm64", Family.LINUX, Architecture.ARM64)

    object LINUX_ARM32_HFP : KonanTarget("linux_arm32_hfp", Family.LINUX, Architecture.ARM32)

    override fun toString() = name

    companion object {
        // TODO: need a better way to enumerated predefined targets.
        val predefinedTargets: Map<String, KonanTarget> by lazy {
            listOf(
                ANDROID_X64, ANDROID_X86, ANDROID_ARM32, ANDROID_ARM64,
                IOS_ARM64, IOS_X64, IOS_SIMULATOR_ARM64,
                WATCHOS_ARM32, WATCHOS_ARM64, WATCHOS_X64,
                WATCHOS_SIMULATOR_ARM64, WATCHOS_DEVICE_ARM64,
                TVOS_ARM64, TVOS_X64, TVOS_SIMULATOR_ARM64,
                LINUX_X64,
                MINGW_X64,
                MACOS_X64, MACOS_ARM64,
                LINUX_ARM64, LINUX_ARM32_HFP
            ).associateBy { it.name }
        }

        // Made lazy to break a class initialization cycle. See KT-82886
        val deprecatedTargets by lazy { setOf(LINUX_ARM32_HFP, IOS_X64, WATCHOS_X64, TVOS_X64, MACOS_X64) }
        val toleratedDeprecatedTargets by lazy { setOf(LINUX_ARM32_HFP, IOS_X64, WATCHOS_X64, TVOS_X64, MACOS_X64) }
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other !is KonanTarget -> false
            else -> this.name == other.name && this.family == other.family && this.architecture == other.architecture
        }
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + family.hashCode()
        result = 31 * result + architecture.hashCode()
        return result
    }
}

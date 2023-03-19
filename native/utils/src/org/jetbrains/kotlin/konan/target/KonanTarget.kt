/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.util.Named
import java.io.Serializable

private const val DEPRECATION_LINK = "https://kotl.in/native-targets-tiers"
const val DEPRECATED_TARGET_MESSAGE = "Target is deprecated and will be removed soon. See: $DEPRECATION_LINK"

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

    // Deprecated targets. TODO: add deprecations
    // Deprecation is not declared right now because otherwise compilation of the project will fail, because warnings will be reported
    object IOS_ARM32 : KonanTarget("ios_arm32", Family.IOS, Architecture.ARM32)
    object WATCHOS_X86 : KonanTarget("watchos_x86", Family.WATCHOS, Architecture.X86)
    object LINUX_ARM32_HFP : KonanTarget("linux_arm32_hfp", Family.LINUX, Architecture.ARM32)
    object LINUX_MIPS32 : KonanTarget("linux_mips32", Family.LINUX, Architecture.MIPS32)
    object LINUX_MIPSEL32 : KonanTarget("linux_mipsel32", Family.LINUX, Architecture.MIPSEL32)
    object MINGW_X86 : KonanTarget("mingw_x86", Family.MINGW, Architecture.X86)
    object WASM32 : KonanTarget("wasm32", Family.WASM, Architecture.WASM32)

    // Tunable targets
    class ZEPHYR(val subName: String, val genericName: String = "zephyr") :
        KonanTarget("${genericName}_$subName", Family.ZEPHYR, Architecture.ARM32)

    override fun toString() = name

    companion object {
        // TODO: need a better way to enumerated predefined targets.
        val predefinedTargets: Map<String, KonanTarget> by lazy {
            listOf(
                ANDROID_X64, ANDROID_X86, ANDROID_ARM32, ANDROID_ARM64,
                IOS_ARM32, IOS_ARM64, IOS_X64, IOS_SIMULATOR_ARM64,
                WATCHOS_ARM32, WATCHOS_ARM64, WATCHOS_X86, WATCHOS_X64,
                WATCHOS_SIMULATOR_ARM64, WATCHOS_DEVICE_ARM64,
                TVOS_ARM64, TVOS_X64, TVOS_SIMULATOR_ARM64,
                LINUX_X64,
                MINGW_X86, MINGW_X64,
                MACOS_X64, MACOS_ARM64,
                LINUX_ARM64, LINUX_ARM32_HFP, LINUX_MIPS32, LINUX_MIPSEL32,
                WASM32
            ).associateBy { it.name }
        }

        val deprecatedTargets = setOf(WATCHOS_X86, IOS_ARM32, LINUX_ARM32_HFP, MINGW_X86, LINUX_MIPS32, LINUX_MIPSEL32, WASM32)
    }
}

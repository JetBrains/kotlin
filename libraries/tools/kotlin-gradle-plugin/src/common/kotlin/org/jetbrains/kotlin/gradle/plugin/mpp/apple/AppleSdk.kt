/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.KonanTarget

internal object AppleSdk {
    fun defineNativeTargets(platform: String, archs: List<String>): List<KonanTarget> {
        class UnknownArchitectureException(platform: String, arch: String) :
            IllegalArgumentException("Architecture $arch is not supported for platform $platform")

        val targets: MutableSet<KonanTarget> = mutableSetOf()

        when {
            platform.startsWith("iphoneos") -> {
                targets.addAll(archs.map { arch ->
                    when (arch) {
                        "arm64", "arm64e" -> KonanTarget.IOS_ARM64
                        else -> throw UnknownArchitectureException(platform, arch)
                    }
                })
            }
            platform.startsWith("iphonesimulator") -> {
                targets.addAll(archs.map { arch ->
                    when (arch) {
                        "arm64", "arm64e" -> KonanTarget.IOS_SIMULATOR_ARM64
                        "x86_64" -> KonanTarget.IOS_X64
                        else -> throw UnknownArchitectureException(platform, arch)
                    }
                })
            }
            platform.startsWith("watchos") -> {
                targets.addAll(archs.map { arch ->
                    when (arch) {
                        "armv7k" -> KonanTarget.WATCHOS_ARM32
                        "arm64_32" -> KonanTarget.WATCHOS_ARM64
                        "arm64" -> KonanTarget.WATCHOS_DEVICE_ARM64
                        else -> throw UnknownArchitectureException(platform, arch)
                    }
                })
            }
            platform.startsWith("watchsimulator") -> {
                targets.addAll(archs.map { arch ->
                    when (arch) {
                        "arm64", "arm64e" -> KonanTarget.WATCHOS_SIMULATOR_ARM64
                        "x86_64" -> KonanTarget.WATCHOS_X64
                        else -> throw UnknownArchitectureException(platform, arch)
                    }
                })
            }
            platform.startsWith("appletvos") -> {
                targets.addAll(archs.map { arch ->
                    when (arch) {
                        "arm64", "arm64e" -> KonanTarget.TVOS_ARM64
                        else -> throw UnknownArchitectureException(platform, arch)
                    }
                })
            }
            platform.startsWith("appletvsimulator") -> {
                targets.addAll(archs.map { arch ->
                    when (arch) {
                        "arm64", "arm64e" -> KonanTarget.TVOS_SIMULATOR_ARM64
                        "x86_64" -> KonanTarget.TVOS_X64
                        else -> throw UnknownArchitectureException(platform, arch)
                    }
                })
            }
            platform.startsWith("macosx") -> {
                targets.addAll(archs.map { arch ->
                    when (arch) {
                        "arm64" -> KonanTarget.MACOS_ARM64
                        "x86_64" -> KonanTarget.MACOS_X64
                        else -> throw UnknownArchitectureException(platform, arch)
                    }
                })
            }
            else -> throw IllegalArgumentException("Platform $platform is not supported")
        }

        return targets.toList()
    }

    val xcodeEnvironmentDebugDylibVars = setOf(
        "ENABLE_DEBUG_DYLIB",
        "EXECUTABLE_BLANK_INJECTION_DYLIB_PATH",
        "EXECUTABLE_DEBUG_DYLIB_INSTALL_NAME",
        "EXECUTABLE_DEBUG_DYLIB_PATH"
    )
}

internal val KonanTarget.appleArchitecture: String
    get() = when (this) {
        KonanTarget.IOS_ARM64,
        KonanTarget.IOS_SIMULATOR_ARM64,
        KonanTarget.MACOS_ARM64,
        KonanTarget.TVOS_ARM64,
        KonanTarget.TVOS_SIMULATOR_ARM64,
        KonanTarget.WATCHOS_DEVICE_ARM64,
        KonanTarget.WATCHOS_SIMULATOR_ARM64,
            -> "arm64"

        KonanTarget.IOS_X64,
        KonanTarget.MACOS_X64,
        KonanTarget.TVOS_X64,
        KonanTarget.WATCHOS_X64,
            -> "x86_64"

        KonanTarget.WATCHOS_ARM32 -> "armv7k"
        KonanTarget.WATCHOS_ARM64 -> "arm64_32"

        else -> throw IllegalArgumentException("Target $this is not an Apple target or not supported yet")
    }

internal val KotlinNativeTarget.appleTarget: AppleTarget
    get() = konanTarget.appleTarget

internal val KonanTarget.appleTarget: AppleTarget
    get() = AppleTarget.values().singleOrNull { it.targets.contains(this) }
        ?: throw IllegalArgumentException("Target $this is not an Apple target or not supported yet")

internal val AppleTarget.applePlatform: String
    get() = when (this) {
        AppleTarget.MACOS_DEVICE -> "macOS"
        AppleTarget.IPHONE_DEVICE -> "iOS"
        AppleTarget.IPHONE_SIMULATOR -> "iOS Simulator"
        AppleTarget.WATCHOS_DEVICE -> "watchOS"
        AppleTarget.WATCHOS_SIMULATOR -> "watchOS Simulator"
        AppleTarget.TVOS_DEVICE -> "tvOS"
        AppleTarget.TVOS_SIMULATOR -> "tvOS Simulator"
    }

internal val AppleTarget.sdk: String
    get() = when (this) {
        AppleTarget.MACOS_DEVICE -> "macosx"
        AppleTarget.IPHONE_DEVICE -> "iphoneos"
        AppleTarget.IPHONE_SIMULATOR -> "iphonesimulator"
        AppleTarget.WATCHOS_DEVICE -> "watchos"
        AppleTarget.WATCHOS_SIMULATOR -> "watchsimulator"
        AppleTarget.TVOS_DEVICE -> "appletvos"
        AppleTarget.TVOS_SIMULATOR -> "appletvsimulator"
    }

internal val KonanTarget.applePlatform: String
    get() = appleTarget.applePlatform


internal val NativeBuildType.configuration: String
    get() = when (this) {
        NativeBuildType.RELEASE -> "Release"
        NativeBuildType.DEBUG -> "Debug"
    }

internal val AppleTarget.genericPlatformDestination: String
    get() = "generic/platform=${applePlatform}"

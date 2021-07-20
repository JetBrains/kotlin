/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.jetbrains.kotlin.konan.target.KonanTarget

internal object AppleSdk {
    fun defineNativeTargets(platform: String, archs: List<String>): List<KonanTarget> {
        class UnknownArchitectureException(platform: String, arch: String) :
            IllegalArgumentException("Architecture $arch is not supported for platform $platform")

        val targets: MutableSet<KonanTarget> = mutableSetOf()

        when (platform) {
            "iphoneos" -> {
                targets.addAll(archs.map { arch ->
                    when (arch) {
                        "arm64", "arm64e" -> KonanTarget.IOS_ARM64
                        "armv7", "armv7s" -> KonanTarget.IOS_ARM32
                        else -> throw UnknownArchitectureException(platform, arch)
                    }
                })
            }
            "iphonesimulator" -> {
                targets.addAll(archs.map { arch ->
                    when (arch) {
                        "arm64", "arm64e" -> KonanTarget.IOS_SIMULATOR_ARM64
                        "x86_64" -> KonanTarget.IOS_X64
                        else -> throw UnknownArchitectureException(platform, arch)
                    }
                })
            }
            "watchos" -> {
                targets.addAll(archs.map { arch ->
                    when (arch) {
                        "armv7k" -> KonanTarget.WATCHOS_ARM32
                        "arm64_32" -> KonanTarget.WATCHOS_ARM64
                        else -> throw UnknownArchitectureException(platform, arch)
                    }
                })
            }
            "watchsimulator" -> {
                targets.addAll(archs.map { arch ->
                    when (arch) {
                        "arm64", "arm64e" -> KonanTarget.WATCHOS_SIMULATOR_ARM64
                        "i386" -> KonanTarget.WATCHOS_X86
                        "x86_64" -> KonanTarget.WATCHOS_X64
                        else -> throw UnknownArchitectureException(platform, arch)
                    }
                })
            }
            "appletvos" -> {
                targets.addAll(archs.map { arch ->
                    when (arch) {
                        "arm64", "arm64e" -> KonanTarget.TVOS_ARM64
                        else -> throw UnknownArchitectureException(platform, arch)
                    }
                })
            }
            "appletvsimulator" -> {
                targets.addAll(archs.map { arch ->
                    when (arch) {
                        "arm64", "arm64e" -> KonanTarget.TVOS_SIMULATOR_ARM64
                        "x86_64" -> KonanTarget.TVOS_X64
                        else -> throw UnknownArchitectureException(platform, arch)
                    }
                })
            }
            "macosx" -> {
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
}
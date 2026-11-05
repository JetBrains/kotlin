/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.toolchain

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainPlatform

/**
 * The operating system names used by [ToolchainPlatform].
 *
 * Every tool publishes its distributions under its own naming scheme, so these values are intentionally
 * tool-independent, and each [ExternalToolchainForKotlin] maps them to the names the tool uses.
 */
internal object ToolchainOs {
    const val LINUX = "linux"
    const val MACOS = "macos"
    const val WINDOWS = "windows"
}

/**
 * The CPU architecture names used by [ToolchainPlatform].
 */
internal object ToolchainArch {
    const val X86_64 = "x86_64"
    const val AARCH64 = "aarch64"
    const val X86 = "x86"
}

internal val ToolchainPlatform.isWindows: Boolean get() = os == ToolchainOs.WINDOWS

/**
 * Appends the `.exe` extension to [command] on Windows.
 */
internal fun ToolchainPlatform.executableName(command: String): String =
    if (isWindows) "$command.exe" else command

/**
 * Detects the operating system and the architecture of the machine running the build.
 */
internal fun ProviderFactory.detectToolchainPlatform(): Provider<ToolchainPlatform> =
    systemProperty("os.name").zip(systemProperty("os.arch")) { osName, osArch ->
        ToolchainPlatform(parseToolchainOs(osName), parseToolchainArch(osArch))
    }

private fun parseToolchainOs(osName: String): String {
    val name = osName.lowercase()
    return when {
        name.contains("windows") -> ToolchainOs.WINDOWS
        name.contains("mac") -> ToolchainOs.MACOS
        name.contains("linux") -> ToolchainOs.LINUX
        name.contains("freebsd") -> ToolchainOs.LINUX
        else -> throw IllegalArgumentException("Unsupported OS: $osName")
    }
}

private fun parseToolchainArch(osArch: String): String {
    val arch = osArch.lowercase()
    return when {
        arch == "aarch64" || arch == "arm64" || arch.startsWith("aarch") -> ToolchainArch.AARCH64
        arch.contains("64") -> ToolchainArch.X86_64
        else -> ToolchainArch.X86
    }
}

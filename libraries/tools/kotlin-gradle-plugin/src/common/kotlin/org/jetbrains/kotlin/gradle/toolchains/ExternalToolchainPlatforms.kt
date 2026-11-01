/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExternalToolchainDsl::class)

package org.jetbrains.kotlin.gradle.toolchains

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.ExternalToolchainPlatform

/**
 * The operating system names used by [ExternalToolchainPlatform].
 *
 * Every tool publishes its distributions under its own naming scheme, so these values are intentionally
 * tool-independent, and each toolchain service maps them to the names the tool uses.
 */
internal object ExternalToolchainOs {
    const val LINUX = "linux"
    const val MACOS = "macos"
    const val WINDOWS = "windows"
}

/**
 * The CPU architecture names used by [ExternalToolchainPlatform].
 */
internal object ExternalToolchainArch {
    const val X86_64 = "x86_64"
    const val AARCH64 = "aarch64"
}

internal val ExternalToolchainPlatform.isWindows: Boolean get() = os == ExternalToolchainOs.WINDOWS

/**
 * Appends the `.exe` extension to [command] on Windows.
 */
internal fun ExternalToolchainPlatform.executableName(command: String): String =
    if (isWindows) "$command.exe" else command

/**
 * Detects the operating system and the architecture of the machine running the build.
 */
internal fun ProviderFactory.detectExternalToolchainPlatform(): Provider<ExternalToolchainPlatform> =
    systemProperty("os.name").zip(systemProperty("os.arch")) { osName, osArch ->
        ExternalToolchainPlatform(parseExternalToolchainOs(osName), parseExternalToolchainArch(osArch))
    }

private fun parseExternalToolchainOs(osName: String): String {
    val name = osName.lowercase()
    return when {
        name.contains("windows") -> ExternalToolchainOs.WINDOWS
        name.contains("mac") -> ExternalToolchainOs.MACOS
        name.contains("linux") -> ExternalToolchainOs.LINUX
        name.contains("freebsd") -> ExternalToolchainOs.LINUX
        else -> throw IllegalArgumentException("Unsupported OS: $osName")
    }
}

private fun parseExternalToolchainArch(osArch: String): String {
    val arch = osArch.lowercase()
    return when {
        arch == "aarch64" || arch == "arm64" || arch.startsWith("aarch") -> ExternalToolchainArch.AARCH64
        arch.contains("64") -> ExternalToolchainArch.X86_64
        else -> throw IllegalArgumentException("Unsupported OS architecture: '$arch'")
    }
}

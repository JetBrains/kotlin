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
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainVersion

/**
 * The operating system names used by [ToolchainPlatform].
 *
 * Every tool publishes its distributions under its own naming scheme, so these values are intentionally
 * tool-independent, and each [ToolchainDistributionLayout] maps them to the names the tool uses.
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
 * The format of the file a tool distribution is published as.
 */
internal enum class ToolchainArchiveType {
    ZIP,
    TAR_GZ,

    /**
     * Gradle cannot read `.tar.xz` archives directly, see https://github.com/gradle/gradle/issues/31858,
     * so such an archive is decompressed into a plain `.tar` first.
     */
    TAR_XZ,

    /**
     * The distribution is a single executable file, published without any archiving.
     */
    RAW_EXECUTABLE,
    ;
}

/**
 * Describes how a tool publishes its distributions, and what an installed distribution looks like on disk.
 *
 * A layout is the only tool-specific part of the download-based toolchain services -
 * [ToolchainDistributionInstaller] handles downloading, unpacking and installing for all tools alike.
 */
internal interface ToolchainDistributionLayout {

    /**
     * The name of the tool, used in log and error messages, for example `Binaryen`.
     */
    val toolName: String

    /**
     * The base URL the distributions are published at.
     */
    val defaultDownloadBaseUrl: String

    /**
     * The name of the directory a distribution is installed into, inside the installations directory,
     * for example `binaryen-version_133-x86_64-linux`.
     *
     * It must identify the distribution completely, because installations are shared between builds.
     */
    fun distributionName(version: ToolchainVersion, platform: ToolchainPlatform): String

    /**
     * The format the distribution is published in.
     */
    fun archiveType(platform: ToolchainPlatform): ToolchainArchiveType

    /**
     * The path of the distribution file, relative to the download base URL.
     */
    fun archivePath(version: ToolchainVersion, platform: ToolchainPlatform): String

    /**
     * The directory inside the archive that holds the distribution, or `null` when the archive content itself
     * is the distribution.
     */
    fun archiveRootDirName(version: ToolchainVersion, platform: ToolchainPlatform): String? = null

    /**
     * The path of the tool executable, relative to the installation directory, for example `bin/wasm-opt`.
     */
    fun executablePath(version: ToolchainVersion, platform: ToolchainPlatform): String

    /**
     * The command used to run the tool when it is not provisioned but taken from the `PATH`,
     * for example `wasm-opt`.
     */
    fun preInstalledCommand(platform: ToolchainPlatform): String
}

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

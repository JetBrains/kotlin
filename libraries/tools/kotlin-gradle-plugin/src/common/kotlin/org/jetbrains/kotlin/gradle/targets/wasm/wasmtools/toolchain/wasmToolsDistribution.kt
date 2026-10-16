/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsBuildPlatform
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsVersion
import org.jetbrains.kotlin.gradle.internal.unameExecResult
import org.jetbrains.kotlin.gradle.targets.js.nodejs.OsType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.parseOsType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.parsePlatform
import java.io.File

/**
 * The `os` value of [WasmToolsBuildPlatform] used by the Windows wasm-tools distributions.
 */
internal const val WINDOWS_OS_NAME = "win"

internal val WasmToolsVersion.normalized: String get() = version.removePrefix("v")

internal val WasmToolsBuildPlatform.isWindows: Boolean get() = os == WINDOWS_OS_NAME

/**
 * The operating system name used by the wasm-tools release assets.
 */
internal fun wasmToolsOsClassifier(platform: WasmToolsBuildPlatform): String = when (platform.os) {
    WINDOWS_OS_NAME -> "windows"
    "darwin" -> "macos"
    "linux" -> "linux"
    else -> throw IllegalArgumentException("Unsupported operating system for wasm-tools: '${platform.os}'")
}

/**
 * The CPU architecture name used by the wasm-tools release assets.
 */
internal fun wasmToolsArchClassifier(platform: WasmToolsBuildPlatform): String = when (platform.arch) {
    "x64" -> "x86_64"
    "arm64" -> "aarch64"
    else -> throw IllegalArgumentException("Unsupported architecture for wasm-tools: '${platform.arch}'")
}

/**
 * The name of the wasm-tools distribution, as used both by the official distribution archives
 * and by the layout of the local installation directory, for example `wasm-tools-1.259.0-x86_64-linux`.
 */
internal fun wasmToolsDistributionName(versionValue: String, osName: String, architecture: String): String =
    "wasm-tools-$versionValue-$architecture-$osName"

internal fun wasmToolsDistributionName(version: WasmToolsVersion, platform: WasmToolsBuildPlatform): String =
    wasmToolsDistributionName(version.normalized, wasmToolsOsClassifier(platform), wasmToolsArchClassifier(platform))

/**
 * The extension of the official distribution archive. Windows distributions are published as ZIP archives.
 */
internal fun wasmToolsArchiveExtension(platform: WasmToolsBuildPlatform): String =
    if (platform.isWindows) "zip" else "tar.gz"

internal fun ProviderFactory.detectWasmToolsPlatform(): Provider<WasmToolsBuildPlatform> {
    val uname = unameExecResult
    return currentHostPlatform().zip(systemProperty("os.arch")) { osType, osArch ->
        val platform = parsePlatform(osType, osArch, uname)
        WasmToolsBuildPlatform(platform.name, platform.arch)
    }
}

internal fun ProviderFactory.currentHostPlatform(): Provider<OsType> = systemProperty("os.name").map {
    parseOsType(it)
}

internal fun ArchiveOperations.extractWasmTools(
    fs: FileSystemOperations,
    archive: File,
    destination: File,
) {
    fs.copy {
        it.from(
            when {
                archive.name.endsWith("zip") -> zipTree(archive)
                else -> tarTree(archive)
            }
        )
        it.into(destination)
    }
}

internal fun setUpWasmTools(logger: Logger, archive: File, destination: File, isWindows: Boolean, executable: File) {
    if (!isWindows) {
        executable.setExecutable(true)
    }
}

/**
 * The wasm-tools executable inside an installed distribution.
 *
 * The release archives put the binary directly in the top-level distribution directory.
 */
internal fun wasmToolsExecutableFile(installationDir: File, platform: WasmToolsBuildPlatform): File =
    installationDir.resolve(if (platform.isWindows) "wasm-tools.exe" else "wasm-tools")

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.internal.unameExecResult
import org.jetbrains.kotlin.gradle.targets.js.nodejs.computeNodeBinDir
import org.jetbrains.kotlin.gradle.targets.js.nodejs.parsePlatform
import java.io.File

/**
 * The `os` value of [BuildPlatform] used by the Windows Node.js distributions.
 */
internal const val WINDOWS_OS_NAME = "win"

/**
 * The oldest Node.js version the Kotlin Gradle Plugin is tested against.
 *
 * Requesting an older version is not forbidden, but produces a warning.
 */
internal const val MINIMAL_SUPPORTED_NODE_JS_MAJOR_VERSION = 18

/**
 * The base URL of the official Node.js distributions.
 */
internal const val OFFICIAL_NODE_JS_DOWNLOAD_BASE_URL = "https://nodejs.org/dist"

internal val NodeJsVersion.normalized: String get() = version.removePrefix("v")

internal val NodeJsVersion.majorVersion: Int? get() = normalized.substringBefore('.').toIntOrNull()

internal val BuildPlatform.isWindows: Boolean get() = os == WINDOWS_OS_NAME

/**
 * The name of the Node.js distribution, as used both by the official distribution archives
 * and by the layout of the local installation directory, for example `node-v24.16.0-linux-x64`.
 */
internal fun nodeJsDistributionName(version: NodeJsVersion, platform: BuildPlatform): String =
    "node-v${version.normalized}-${platform.os}-${platform.arch}"

/**
 * The extension of the official distribution archive. Windows distributions are published as ZIP archives.
 */
internal fun nodeJsArchiveExtension(platform: BuildPlatform): String =
    if (platform.isWindows) "zip" else "tar.gz"

/**
 * The Node.js executable inside an installed distribution.
 */
internal fun nodeJsExecutableFile(installationDir: File, platform: BuildPlatform): File {
    val binDir = computeNodeBinDir(installationDir.toPath(), platform.isWindows).toFile()
    return binDir.resolve(if (platform.isWindows) "node.exe" else "node")
}

/**
 * Detects the [BuildPlatform] of the machine running the build.
 */
internal fun ProviderFactory.detectBuildPlatform(): Provider<BuildPlatform> {
    val uname = unameExecResult
    return systemProperty("os.name").zip(systemProperty("os.arch")) { osName, osArch ->
        val platform = parsePlatform(osName, osArch, uname)
        BuildPlatform(platform.name, platform.arch)
    }
}

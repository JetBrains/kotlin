/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.binaryen

/**
 * Provides platform and architecture names that is used to download Binaryen.
 */
internal data class BinaryenPlatform(
    val name: String,
    val arch: String,
) {
    val platform: String
        get() = "$arch-$name"

    fun isWindows(): Boolean {
        return name == WIN
    }

    companion object {
        const val WIN = "windows"
        const val LINUX = "linux"
        const val DARWIN = "macos"

        const val X64 = "x86_64"
        const val ARM64 = "arm64"
        const val AARCH64 = "aarch64"

        internal val SUPPORTED_PLATFORMS = listOf(
            "$X64-$WIN",
            "$ARM64-$WIN",
            "$X64-$DARWIN",
            "$ARM64-$DARWIN",
            "$X64-$LINUX",
            "$AARCH64-$LINUX",
        )

        internal fun parseBinaryenPlatformOrNull(name: String, arch: String): BinaryenPlatform? {
            val osName = parseOsName(name.lowercase()) ?: return null
            val osArch = parseOsArch(osName, arch.lowercase()) ?: return null
            return BinaryenPlatform(osName, osArch)
        }

        private fun parseOsName(name: String): String? {
            return when {
                name.contains("windows") -> WIN
                name.contains("mac") -> DARWIN
                name.contains("linux") -> LINUX
                name.contains("freebsd") -> LINUX
                else -> null
            }
        }

        private fun parseOsArch(osName: String, arch: String): String? {
            return when {
                // Binaryen names 64-bit ARM archives 'aarch64' on Linux, but 'arm64' on Windows and macOS
                arch == "arm" || arch.startsWith("aarch") -> if (osName == LINUX) AARCH64 else ARM64
                arch.contains("64") -> X64
                // Binaryen does not publish 32-bit x86 archives
                else -> null
            }
        }
    }
}

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.swc

/**
 * Provides platform and architecture names used to download Swc.
 */
internal data class SwcPlatform(
    val name: String,
    val arch: String,
) {
    val cStdlib: String?
        get() = when (name) {
            LINUX -> GNU_LIB
            WIN -> MSVC
            else -> null
        }

    val extension: String
        get() = when (name) {
            WIN -> ".exe"
            else -> ""
        }

    val classifier: String
        get() = "$name-$arch${cStdlib?.let { "-$it" } ?: ""}$extension"

    fun isWindows(): Boolean {
        return name == WIN
    }

    companion object {
        const val WIN = "win32"
        const val LINUX = "linux"
        const val DARWIN = "darwin"

        const val X64 = "x64"
        const val X32 = "ia32"
        const val ARM = "arm64"

        const val GNU_LIB = "gnu"
        const val MSVC = "msvc"

        internal fun parseSwcPlatform(name: String, arch: String): SwcPlatform {
            return SwcPlatform(
                parseOsName(name.lowercase(java.util.Locale.ROOT)),
                parseOsArch(arch.lowercase(java.util.Locale.ROOT))
            )
        }

        private fun parseOsName(name: String): String {
            return when {
                name.contains("windows") -> WIN
                name.contains("mac") -> DARWIN
                name.contains("linux") -> LINUX
                name.contains("freebsd") -> LINUX
                else -> error("Unsupported platform: $name")
            }
        }

        private fun parseOsArch(arch: String): String {
            return when {
                arch == "arm" || arch.startsWith("aarch") -> ARM
                arch.contains("64") -> X64
                else -> X32
            }
        }
    }
}
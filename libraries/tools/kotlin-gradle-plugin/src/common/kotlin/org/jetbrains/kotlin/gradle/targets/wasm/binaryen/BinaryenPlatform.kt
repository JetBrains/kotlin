/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.binaryen

import org.gradle.api.provider.Provider

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
        const val X86 = "x86_86"

        internal fun parseBinaryenPlatform(name: String, arch: String, uname: Provider<String>): BinaryenPlatform {
            return BinaryenPlatform(
                parseOsName(name.lowercase()),
                parseOsArch(
                    arch.lowercase(),
                    uname
                )
            )
        }

        private fun parseOsName(name: String): String {
            return when {
                name.contains("windows") -> WIN
                name.contains("mac") -> DARWIN
                name.contains("linux") -> LINUX
                name.contains("freebsd") -> LINUX
                else -> throw IllegalArgumentException("Unsupported OS: $name")
            }
        }

        private fun parseOsArch(arch: String, uname: Provider<String>): String {
            return when {
                arch == "arm" || arch.startsWith("aarch") -> uname.get()
                arch.contains("64") -> X64
                else -> X86
            }
        }
    }
}
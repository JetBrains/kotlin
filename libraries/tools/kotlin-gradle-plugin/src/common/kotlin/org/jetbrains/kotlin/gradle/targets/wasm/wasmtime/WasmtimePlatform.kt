/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtime

/**
 * Provides platform and architecture names that are used to download Wasmtime.
 *
 * See https://github.com/bytecodealliance/wasmtime/releases for the available archives.
 */
internal object WasmtimePlatform {
    private val props = System.getProperties()
    private fun property(name: String) = props.getProperty(name) ?: System.getProperty(name)

    const val WINDOWS = "windows"
    const val LINUX = "linux"
    const val MACOS = "macos"

    val name: String
        get() {
            val osName = property("os.name").lowercase()
            return when {
                osName.contains("windows") -> WINDOWS
                osName.contains("mac") -> MACOS
                osName.contains("linux") -> LINUX
                osName.contains("freebsd") -> LINUX
                else -> throw IllegalArgumentException("Unsupported OS for Wasmtime: $osName")
            }
        }

    const val AARCH64 = "aarch64"
    const val X86_64 = "x86_64"

    val architecture: String
        get() {
            val arch = property("os.arch").lowercase()
            return when {
                arch == "aarch64" || arch == "arm64" -> AARCH64
                arch.contains("64") -> X86_64
                else -> throw IllegalArgumentException("Unsupported architecture for Wasmtime: $arch")
            }
        }

    /**
     * Platform identifier used by Wasmtime release artifacts, e.g. `x86_64-linux`, `aarch64-macos`.
     */
    val platform: String
        get() = "$architecture-$name"

    /**
     * Archive extension used by Wasmtime release artifacts on the current platform.
     */
    val archiveExtension: String
        get() = if (name == WINDOWS) "zip" else "tar.xz"
}

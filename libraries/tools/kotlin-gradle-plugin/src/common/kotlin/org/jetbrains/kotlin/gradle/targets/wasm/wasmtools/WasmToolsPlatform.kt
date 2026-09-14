/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtools

/**
 * Provides platform and architecture names that are used to download `wasm-tools`.
 *
 * See https://github.com/bytecodealliance/wasm-tools/releases for the available archives.
 */
internal object WasmToolsPlatform {
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
                else -> throw IllegalArgumentException("Unsupported OS for wasm-tools: $osName")
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
                else -> throw IllegalArgumentException("Unsupported architecture for wasm-tools: $arch")
            }
        }

    /**
     * Platform identifier used by `wasm-tools` release artifacts, e.g. `x86_64-linux`, `aarch64-macos`.
     */
    val platform: String
        get() = "$architecture-$name"

    /**
     * Archive extension used by `wasm-tools` release artifacts on the current platform.
     */
    val archiveExtension: String
        get() = if (name == WINDOWS) "zip" else "tar.gz"
}

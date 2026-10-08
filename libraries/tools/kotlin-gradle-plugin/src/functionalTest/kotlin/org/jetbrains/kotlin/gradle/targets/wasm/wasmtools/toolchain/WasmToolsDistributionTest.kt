/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsBuildPlatform
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsVersion
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WasmToolsDistributionTest {

    private val version = WasmToolsVersion("1.259.0")

    @Test
    fun linuxX64Distribution() {
        val platform = WasmToolsBuildPlatform("linux", "x64")
        assertEquals("wasm-tools-1.259.0-x86_64-linux", wasmToolsDistributionName(version, platform))
        assertEquals("tar.gz", wasmToolsArchiveExtension(platform))
    }

    @Test
    fun macOsArm64Distribution() {
        val platform = WasmToolsBuildPlatform("darwin", "arm64")
        assertEquals("wasm-tools-1.259.0-aarch64-macos", wasmToolsDistributionName(version, platform))
        assertEquals("tar.gz", wasmToolsArchiveExtension(platform))
    }

    @Test
    fun windowsX64Distribution() {
        val platform = WasmToolsBuildPlatform("win", "x64")
        assertEquals("wasm-tools-1.259.0-x86_64-windows", wasmToolsDistributionName(version, platform))
        assertEquals("zip", wasmToolsArchiveExtension(platform))
    }

    @Test
    fun versionIsNormalized() {
        assertEquals("1.259.0", WasmToolsVersion("v1.259.0").normalized)
    }

    @Test
    fun executableIsInTheInstallationRoot() {
        val installationDir = File("/tmp/wasm-tools-1.259.0-x86_64-linux")
        assertEquals(
            installationDir.resolve("wasm-tools"),
            wasmToolsExecutableFile(installationDir, WasmToolsBuildPlatform("linux", "x64"))
        )
        assertEquals(
            installationDir.resolve("wasm-tools.exe"),
            wasmToolsExecutableFile(installationDir, WasmToolsBuildPlatform("win", "x64"))
        )
    }

    @Test
    fun unsupportedArchitectureFails() {
        assertFailsWith<IllegalArgumentException> {
            wasmToolsDistributionName(version, WasmToolsBuildPlatform("linux", "s390x"))
        }
    }

    @Test
    fun unsupportedOperatingSystemFails() {
        assertFailsWith<IllegalArgumentException> {
            wasmToolsDistributionName(version, WasmToolsBuildPlatform("solaris", "x64"))
        }
    }
}

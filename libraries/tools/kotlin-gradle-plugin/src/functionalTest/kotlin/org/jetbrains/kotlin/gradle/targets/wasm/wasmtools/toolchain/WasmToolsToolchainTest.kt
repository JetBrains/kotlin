/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.PreInstalledWasmToolsToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsBuildPlatform
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsVersion
import org.jetbrains.kotlin.gradle.util.buildProject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

@OptIn(ExperimentalWasmDsl::class)
class WasmToolsToolchainTest {

    private fun buildProjectAndDsl(): Pair<Project, WasmToolsToolchainManagementDsl> =
        buildProject().let { project -> project to WasmToolsToolchainManagementDslImpl(project.gradle) }

    private fun registeredService(project: Project) = project.gradle.sharedServices.registrations
        .getByName(wasmToolsServiceName)
        .service
        .get()

    @Test
    fun `test parse wasm-tools version output variants`() {
        // Standard official release output
        assertEquals(
            WasmToolsVersion("1.259.0"),
            parseWasmToolsVersion("wasm-tools 1.259.0")
        )

        // Official release output with git commit hash and build date (KT-91208)
        assertEquals(
            WasmToolsVersion("1.259.0"),
            parseWasmToolsVersion("wasm-tools 1.259.0 (0a1b2c3d 2026-11-01)")
        )

        // Version with 'v' prefix
        assertEquals(
            WasmToolsVersion("1.259.0").normalized,
            parseWasmToolsVersion("wasm-tools v1.259.0")?.normalized
        )

        // Version like v100.0.0 (vInt.Int.Int pattern)
        assertEquals(
            WasmToolsVersion("v100.0.0"),
            parseWasmToolsVersion("v100.0.0")
        )
        assertEquals(
            WasmToolsVersion("v100.0.0"),
            parseWasmToolsVersion("wasm-tools v100.0.0")
        )

        // Raw version number output
        assertEquals(
            WasmToolsVersion("1.259.0"),
            parseWasmToolsVersion("1.259.0")
        )

        // Pre-release version with commit information
        assertEquals(
            WasmToolsVersion("1.259.0-alpha.1"),
            parseWasmToolsVersion("wasm-tools 1.259.0-alpha.1 (abcdef 2026-10-15)")
        )

        // Multi-line or whitespace-padded output
        assertEquals(
            WasmToolsVersion("1.259.0"),
            parseWasmToolsVersion("\n  wasm-tools 1.259.0 (0a1b2c3d 2026-11-01)\n")
        )

        assertEquals(
            WasmToolsVersion("1.259.0"),
            parseWasmToolsVersion("wasm-tools\r\n1.259.0")
        )

        assertEquals(
            WasmToolsVersion("1.259.0"),
            parseWasmToolsVersion("wasm-tools\t1.259.0")
        )

        // Invalid / unparseable output
        assertNull(parseWasmToolsVersion("command not found"))
        assertNull(parseWasmToolsVersion(""))
    }

    @Test
    fun `test WasmToolsVersion value semantics and string representation`() {
        val version1 = WasmToolsVersion("1.259.0")
        val version2 = WasmToolsVersion("1.259.0")
        val version3 = WasmToolsVersion("1.250.0")

        assertEquals(version1, version2)
        assertEquals(version1.hashCode(), version2.hashCode())
        assertNotEquals(version1, version3)

        // toString() must return the version string directly for clean diagnostic messages
        assertEquals("1.259.0", version1.toString())
        assertEquals("1.259.0", version1.normalized)
        assertEquals("1.259.0", WasmToolsVersion("v1.259.0").normalized)
    }

    @Test
    fun `test WasmToolsBuildPlatform value semantics and string representation`() {
        val platform1 = WasmToolsBuildPlatform("linux", "x64")
        val platform2 = WasmToolsBuildPlatform("linux", "x64")
        val platform3 = WasmToolsBuildPlatform("darwin", "arm64")

        assertEquals(platform1, platform2)
        assertEquals(platform1.hashCode(), platform2.hashCode())
        assertNotEquals(platform1, platform3)

        assertEquals("linux-x64", platform1.toString())
        assertEquals("darwin-arm64", platform3.toString())
    }

    @Test
    fun `toolchainService with PreInstalledWasmToolsToolchainService configures that service`() {
        val (project, dsl) = buildProjectAndDsl()

        dsl.toolchainService(PreInstalledWasmToolsToolchainService::class) {
            wasmToolsExecutable.set("/opt/bin/wasm-tools")
        }

        registerWasmToolsToolchainServiceIfAbsent(project)

        val service = assertIs<PreInstalledWasmToolsToolchainServiceImpl>(registeredService(project))
        assertEquals("/opt/bin/wasm-tools", service.parameters.wasmToolsExecutable.get())
    }

    @Test
    fun `toolchainService with an Action configures the default service`() {
        val (project, dsl) = buildProjectAndDsl()

        dsl.toolchainService { parameters ->
            parameters.downloadBaseUrl.set("https://example.com/wasm-tools")
        }

        registerWasmToolsToolchainServiceIfAbsent(project)

        val service = assertIs<DefaultWasmToolsToolchainServiceImpl>(registeredService(project))
        assertEquals("https://example.com/wasm-tools", service.parameters.downloadBaseUrl.get())
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.toolchain

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.DownloadedToolchainParameters
import org.jetbrains.kotlin.gradle.dsl.toolchain.KotlinToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.PreInstalledToolchainParameters
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.PreInstalledBinaryenToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.d8.DefaultD8ToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.d8.PreInstalledD8ToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.swc.PreInstalledSwcToolchainService
import org.jetbrains.kotlin.gradle.toolchains.*
import org.jetbrains.kotlin.gradle.util.buildProject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers the `toolchainManagement { d8 { ... } }` and friends part of the `org.jetbrains.kotlin.ecosystem`
 * settings plugin, for all four tools that use a [KotlinToolchainService].
 */
class WasmToolchainManagementDslTest {

    private class Tool(
        val name: String,
        val serviceName: String,
        val dsl: (Project) -> ToolchainManagementDsl,
        val register: (Project) -> Unit,
        val downloadedImpl: Class<*>,
        val disabledImpl: Class<*>,
    ) {
        fun service(project: Project): KotlinToolchainService<*> =
            project.gradle.sharedServices.registrations.getByName(serviceName).service.get()
                    as KotlinToolchainService<*>

        fun assertServiceIs(expected: Class<*>, project: Project) {
            val service = service(project)
            assertTrue(
                expected.isInstance(service),
                "Expected the $name service to be a ${expected.simpleName}, but it was ${service.javaClass.name}",
            )
        }
    }

    private val tools = listOf(
        Tool(
            name = "d8",
            serviceName = d8Toolchain.serviceName,
            dsl = { D8ToolchainManagementDslImpl(it.gradle) },
            register = { d8Toolchain.registerIfAbsent(it) },
            downloadedImpl = DefaultD8ToolchainServiceImpl::class.java,
            disabledImpl = DisabledD8ToolchainServiceImpl::class.java,
        ),
        Tool(
            name = "binaryen",
            serviceName = binaryenServiceName,
            dsl = { BinaryenToolchainManagementDslImpl(it.gradle) },
            register = { registerBinaryenToolchainServiceIfAbsent(it) },
            downloadedImpl = DefaultBinaryenToolchainServiceImpl::class.java,
            disabledImpl = DisabledBinaryenToolchainServiceImpl::class.java,
        ),
        Tool(
            name = "wasmtime",
            serviceName = wasmtimeToolchain.serviceName,
            dsl = { WasmtimeToolchainManagementDslImpl(it.gradle) },
            register = { wasmtimeToolchain.registerIfAbsent(it) },
            downloadedImpl = DefaultWasmtimeToolchainServiceImpl::class.java,
            disabledImpl = DisabledWasmtimeToolchainServiceImpl::class.java,
        ),
        Tool(
            name = "swc",
            serviceName = swcToolchain.serviceName,
            dsl = { SwcToolchainManagementDslImpl(it.gradle) },
            register = { swcToolchain.registerIfAbsent(it) },
            downloadedImpl = DefaultSwcToolchainServiceImpl::class.java,
            disabledImpl = DisabledSwcToolchainServiceImpl::class.java,
        ),
    )

    @Test
    fun `toolchainService without a class configures the download-based service`() {
        tools.forEach { tool ->
            val project = buildProject()

            tool.dsl(project).toolchainService {
                downloadBaseUrl.set("https://example.com/${tool.name}")
            }
            tool.register(project)

            tool.assertServiceIs(tool.downloadedImpl, project)
            val parameters = tool.service(project).parameters as DownloadedToolchainParameters
            assertEquals("https://example.com/${tool.name}", parameters.downloadBaseUrl.get())
        }
    }

    @Test
    fun `toolchainService without parameters has a default installationDir`() {
        tools.forEach { tool ->
            val project = buildProject()

            tool.dsl(project).toolchainService { }
            tool.register(project)

            val parameters = tool.service(project).parameters as DownloadedToolchainParameters
            assertTrue(parameters.installationDir.isPresent, "${tool.name} installationDir has no default value")
        }
    }

    @Test
    fun `disable registers the disabled service`() {
        tools.forEach { tool ->
            val project = buildProject()

            tool.dsl(project).disable()
            tool.register(project)

            tool.assertServiceIs(tool.disabledImpl, project)
        }
    }

    @Test
    fun `the toolchain is disabled when the settings plugin does not configure it`() {
        tools.forEach { tool ->
            val project = buildProject()

            tool.register(project)

            tool.assertServiceIs(tool.disabledImpl, project)
        }
    }

    @Test
    fun `configuring the toolchain service a second time fails`() {
        tools.forEach { tool ->
            val dsl = tool.dsl(buildProject())
            dsl.toolchainService { }

            assertFailsWith<IllegalStateException>("${tool.name} allowed a second configuration") {
                dsl.disable()
            }
        }
    }

    @Test
    fun `the built-in service interface is mapped to its implementation`() {
        val project = buildProject()

        D8ToolchainManagementDslImpl(project.gradle).toolchainService(DefaultD8ToolchainService::class) {
            edition.set("dbg")
        }
        d8Toolchain.registerIfAbsent(project)

        val service = assertIs<DefaultD8ToolchainServiceImpl>(
            project.gradle.sharedServices.registrations.getByName(d8Toolchain.serviceName).service.get()
        )
        assertEquals("dbg", service.parameters.edition.get())
    }

    @Test
    fun `d8 toolchainService with a class selects and configures that service`() {
        val project = buildProject()

        D8ToolchainManagementDslImpl(project.gradle).toolchainService(PreInstalledD8ToolchainService::class) {
            executable.set("/opt/v8/d8")
        }
        d8Toolchain.registerIfAbsent(project)

        val service = assertIs<PreInstalledD8ToolchainServiceImpl>(
            project.gradle.sharedServices.registrations.getByName(d8Toolchain.serviceName).service.get()
        )
        assertEquals("/opt/v8/d8", service.parameters.executable.get())
    }

    @Test
    fun `binaryen toolchainService with a class selects and configures that service`() {
        val project = buildProject()

        BinaryenToolchainManagementDslImpl(project.gradle)
            .toolchainService(PreInstalledBinaryenToolchainService::class) {
                executable.set("/usr/local/bin/wasm-opt")
            }
        registerBinaryenToolchainServiceIfAbsent(project)

        val service = assertIs<PreInstalledBinaryenToolchainServiceImpl>(
            project.gradle.sharedServices.registrations.getByName(binaryenServiceName).service.get()
        )
        assertEquals("/usr/local/bin/wasm-opt", service.parameters.executable.get())
    }

    @Test
    fun `preinstalled parameters default to the command of the tool`() {
        val project = buildProject()

        SwcToolchainManagementDslImpl(project.gradle).toolchainService(PreInstalledSwcToolchainService::class) { }
        swcToolchain.registerIfAbsent(project)

        val service = project.gradle.sharedServices.registrations
            .getByName(swcToolchain.serviceName).service.get() as KotlinToolchainService<*>
        assertEquals("swc", (service.parameters as PreInstalledToolchainParameters).executable.get())
    }
}

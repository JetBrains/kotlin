/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.DisabledWasmToolsToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.PreInstalledWasmToolsToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsToolchainManagementDsl
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.nodeJsServiceName
import org.jetbrains.kotlin.gradle.util.buildProject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

@ExperimentalWasmDsl
class WasmToolsToolchainManagementDslTest {

    private fun buildProjectWithMode(mode: String? = null): Project = buildProject(configureProject = {
        if (mode != null) {
            extensions.extraProperties.set("kotlin.wasm.wasmtools.toolchain", mode)
        }
    })

    private fun buildProjectAndDsl(): Pair<Project, WasmToolsToolchainManagementDsl> =
        buildProjectWithMode().let { project -> project to WasmToolsToolchainManagementDslImpl(project.gradle) }

    private fun registeredService(project: Project) = project.gradle.sharedServices.registrations
        .getByName(wasmToolsServiceName)
        .service
        .get()

    @Test
    fun `without any configuration the download based service is registered`() {
        val project = buildProjectWithMode()

        registerWasmToolsToolchainServiceIfAbsent(project)

        assertIs<DefaultWasmToolsToolchainServiceImpl>(registeredService(project))
    }

    @Test
    fun `preinstalled mode property registers the pre-installed service`() {
        val project = buildProjectWithMode("preinstalled")

        registerWasmToolsToolchainServiceIfAbsent(project)

        assertIs<PreInstalledWasmToolsToolchainServiceImpl>(registeredService(project))
    }

    @Test
    fun `disable mode property registers the disabled service`() {
        val project = buildProjectWithMode("disable")

        registerWasmToolsToolchainServiceIfAbsent(project)

        assertIs<DisabledWasmToolsToolchainServiceImpl>(registeredService(project))
    }

    @Test
    fun `toolchainService without a class configures the default DefaultWasmToolsToolchainService`() {
        val (project, dsl) = buildProjectAndDsl()

        dsl.toolchainService {
            downloadBaseUrl.set("https://example.com/dist")
        }
        registerWasmToolsToolchainServiceIfAbsent(project)

        val service = assertIs<DefaultWasmToolsToolchainServiceImpl>(registeredService(project))
        assertEquals("https://example.com/dist", service.parameters.downloadBaseUrl.get())
    }

    @Test
    fun `the settings DSL wins over the mode property`() {
        val project = buildProjectWithMode("disable")
        val dsl = WasmToolsToolchainManagementDslImpl(project.gradle)

        dsl.toolchainService {
            downloadBaseUrl.set("https://example.com/dist")
        }
        registerWasmToolsToolchainServiceIfAbsent(project)

        val service = assertIs<DefaultWasmToolsToolchainServiceImpl>(registeredService(project))
        assertEquals("https://example.com/dist", service.parameters.downloadBaseUrl.get())
    }

    @Test
    fun `toolchainService with a class selects and configures that service`() {
        val (project, dsl) = buildProjectAndDsl()

        dsl.toolchainService(PreInstalledWasmToolsToolchainService::class) {
            wasmToolsExecutable.set("/opt/wasm-tools/bin/wasm-tools")
        }
        registerWasmToolsToolchainServiceIfAbsent(project)

        val service = assertIs<PreInstalledWasmToolsToolchainServiceImpl>(registeredService(project))
        assertEquals("/opt/wasm-tools/bin/wasm-tools", service.parameters.wasmToolsExecutable.get())
    }

    @Test
    fun `disable registers the DisabledWasmToolsToolchainService`() {
        val (project, dsl) = buildProjectAndDsl()

        dsl.disable()
        registerWasmToolsToolchainServiceIfAbsent(project)

        assertIs<DisabledWasmToolsToolchainServiceImpl>(registeredService(project))
    }

    @Test
    fun `disable is a shorthand for toolchainService with DisabledWasmToolsToolchainService`() {
        val (project, dsl) = buildProjectAndDsl()

        dsl.toolchainService(DisabledWasmToolsToolchainService::class) { }
        registerWasmToolsToolchainServiceIfAbsent(project)

        assertIs<DisabledWasmToolsToolchainServiceImpl>(registeredService(project))
    }

    @Test
    fun `configuring the toolchain service a second time fails`() {
        val (_, dsl) = buildProjectAndDsl()

        dsl.toolchainService { }

        assertFailsWith<IllegalStateException> {
            dsl.disable()
        }
    }

    @Test
    fun `disabling and then configuring the toolchain service fails`() {
        val (_, dsl) = buildProjectAndDsl()

        dsl.disable()

        assertFailsWith<IllegalStateException> {
            dsl.toolchainService(PreInstalledWasmToolsToolchainServiceImpl::class) { }
        }
    }

    @Test
    fun `the wasm-tools and the Node js services are registered under different names`() {
        assertNotEquals(nodeJsServiceName, wasmToolsServiceName)
    }
}

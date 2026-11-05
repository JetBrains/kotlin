/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class, ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.toolchain

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenExec
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Exec
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimeExec
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesBinaryenToolchainService
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesD8ToolchainService
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesWasmtimeToolchainService
import org.jetbrains.kotlin.gradle.toolchains.*
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WasmToolchainTaskWiringTest {

    private fun wasmJsProject(preApplyCode: Project.() -> Unit = {}): Project {
        val project = buildProjectWithMPP(preApplyCode = preApplyCode) {
            kotlin {
                wasmJs {
                    d8()
                    binaries.executable()
                }
            }
        }
        project.evaluate()
        return project
    }

    private fun Project.registeredService(name: String): Any =
        gradle.sharedServices.registrations.getByName(name).service.get()

    @Test
    fun `d8 and binaryen tasks use their toolchain services`() {
        val project = wasmJsProject()

        val d8Tasks = project.tasks.withType(D8Exec::class.java).toList()
        assertTrue(d8Tasks.isNotEmpty(), "Expected at least one D8Exec task")
        d8Tasks.forEach { task ->
            assertIs<UsesD8ToolchainService>(task)
            assertTrue(task.d8ToolchainService.isPresent, "Expected ${task.name} to have the D8 toolchain service")
            assertEquals(DEFAULT_D8_VERSION, task.d8Request.get().version.get().version)
        }

        val binaryenTasks = project.tasks.withType(BinaryenExec::class.java).toList()
        assertTrue(binaryenTasks.isNotEmpty(), "Expected at least one BinaryenExec task")
        binaryenTasks.forEach { task ->
            assertIs<UsesBinaryenToolchainService>(task)
            assertTrue(
                task.binaryenToolchainService.isPresent,
                "Expected ${task.name} to have the Binaryen toolchain service",
            )
            assertEquals(DEFAULT_BINARYEN_VERSION, task.binaryenRequest.get().version.get().version)
        }
    }

    @Test
    fun `toolchains are disabled by default, so the legacy setup still provisions the tools`() {
        val project = wasmJsProject()

        assertIs<DisabledD8ToolchainServiceImpl>(project.registeredService(d8Toolchain.serviceName))
        assertIs<DisabledBinaryenToolchainServiceImpl>(project.registeredService(binaryenServiceName))
        assertIs<DisabledSwcToolchainServiceImpl>(project.registeredService(swcToolchain.serviceName))

        assertTrue(
            project.tasks.names.contains("kotlinWasmD8Setup"),
            "Expected the legacy D8 setup task when the toolchain is disabled",
        )
        assertTrue(
            project.tasks.names.contains("kotlinWasmBinaryenSetup"),
            "Expected the legacy Binaryen setup task when the toolchain is disabled",
        )

        val d8Task = project.tasks.withType(D8Exec::class.java).first()
        assertTrue(d8Task.legacyExecutable.isPresent, "Expected the legacy D8 executable to be set")
    }

    @Test
    fun `enabling the d8 toolchain removes the setup task and the legacy executable`() {
        val project = wasmJsProject {
            propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_WASM_D8_TOOLCHAIN_MODE, "download")
            propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_MODE, "download")
            propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_JS_SWC_TOOLCHAIN_MODE, "download")
        }

        assertIs<DefaultD8ToolchainServiceImpl>(project.registeredService(d8Toolchain.serviceName))
        assertIs<DefaultBinaryenToolchainServiceImpl>(project.registeredService(binaryenServiceName))
        assertIs<DefaultSwcToolchainServiceImpl>(project.registeredService(swcToolchain.serviceName))

        assertTrue(
            !project.tasks.names.contains("kotlinWasmD8Setup"),
            "Expected no legacy D8 setup task when the toolchain is enabled",
        )
        assertTrue(
            !project.tasks.names.contains("kotlinWasmBinaryenSetup"),
            "Expected no legacy Binaryen setup task when the toolchain is enabled",
        )

        project.tasks.withType(D8Exec::class.java).forEach { task ->
            assertNull(task.executable, "Expected ${task.name} to resolve D8 only at execution time")
            assertTrue(!task.legacyExecutable.isPresent, "Expected no legacy executable when the toolchain is enabled")
        }
        project.tasks.withType(BinaryenExec::class.java).forEach { task ->
            assertNull(task.executable, "Expected ${task.name} to resolve Binaryen only at execution time")
            assertTrue(!task.legacyExecutable.isPresent, "Expected no legacy executable when the toolchain is enabled")
        }
    }

    @Test
    fun `wasmtime tasks use the wasmtime toolchain service`() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmWasi {
                    wasmtime()
                    binaries.executable()
                }
            }
        }
        project.evaluate()

        val tasks = project.tasks.withType(WasmtimeExec::class.java).toList()
        assertTrue(tasks.isNotEmpty(), "Expected at least one WasmtimeExec task")
        tasks.forEach { task ->
            assertIs<UsesWasmtimeToolchainService>(task)
            assertTrue(
                task.wasmtimeToolchainService.isPresent,
                "Expected ${task.name} to have the Wasmtime toolchain service",
            )
            assertEquals(DEFAULT_WASMTIME_VERSION, task.wasmtimeRequest.get().version.get().version)
        }
        assertIs<DisabledWasmtimeToolchainServiceImpl>(project.registeredService(wasmtimeToolchain.serviceName))
    }

    @Test
    fun `enabling the wasmtime toolchain removes its setup task`() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                propertiesExtension.set(
                    PropertiesProvider.PropertyNames.KOTLIN_WASM_WASMTIME_TOOLCHAIN_MODE,
                    "download",
                )
            }
        ) {
            kotlin {
                wasmWasi {
                    wasmtime()
                    binaries.executable()
                }
            }
        }
        project.evaluate()

        assertIs<DefaultWasmtimeToolchainServiceImpl>(project.registeredService(wasmtimeToolchain.serviceName))
        assertTrue(
            !project.tasks.names.contains("kotlinWasmWasmtimeSetup"),
            "Expected no legacy Wasmtime setup task when the toolchain is enabled, got ${project.tasks.names}",
        )
        project.tasks.withType(WasmtimeExec::class.java).forEach { task ->
            assertNull(task.executable, "Expected ${task.name} to resolve Wasmtime only at execution time")
            assertTrue(!task.legacyExecutable.isPresent, "Expected no legacy executable when the toolchain is enabled")
        }
    }

    @Test
    fun `the d8 toolchain does not configure the root project`() {
        val rootProject = buildProjectWithMPP(
            preApplyCode = {
                propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_WASM_D8_TOOLCHAIN_MODE, "download")
            }
        ) {
            kotlin {
                wasmJs { d8() }
            }
        }
        rootProject.evaluate()

        @Suppress("DEPRECATION_ERROR")
        val d8RootExtensionName = org.jetbrains.kotlin.gradle.targets.wasm.d8.D8RootExtension.EXTENSION_NAME
        assertNull(
            rootProject.extensions.findByName(d8RootExtensionName),
            "The D8 toolchain must not create the root project extension, it breaks Isolated Projects",
        )
    }
}

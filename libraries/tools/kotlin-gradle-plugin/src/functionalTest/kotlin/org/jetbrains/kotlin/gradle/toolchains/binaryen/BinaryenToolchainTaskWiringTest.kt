/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExternalToolchainDsl::class, ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.toolchains.binaryen

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.BinaryenToolchainService
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenExec
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesBinaryenToolchainService
import org.jetbrains.kotlin.gradle.toolchains.externalToolchainServiceName
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BinaryenToolchainTaskWiringTest {

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
    fun `binaryen tasks use the toolchain service`() {
        val project = wasmJsProject()

        val binaryenTasks = project.tasks.withType(BinaryenExec::class.java).toList()
        assertTrue(binaryenTasks.isNotEmpty(), "Expected at least one BinaryenExec task")
        binaryenTasks.forEach { task ->
            assertIs<UsesBinaryenToolchainService>(task)
            assertTrue(
                task.binaryenToolchainService.isPresent,
                "Expected ${task.name} to have the Binaryen toolchain service",
            )
            assertEquals(DEFAULT_BINARYEN_VERSION, task.binaryenToolchainRequest.get().version.get())
        }
    }

    @Test
    fun `binaryen task requests use the configured default version`() {
        val project = wasmJsProject {
            propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_MODE, "download")
            propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_BINARYEN_DEFAULT_VERSION, "134")
        }

        val binaryenTasks = project.tasks.withType(BinaryenExec::class.java).toList()
        assertTrue(binaryenTasks.isNotEmpty(), "Expected at least one BinaryenExec task")
        binaryenTasks.forEach { task ->
            assertEquals("134", task.binaryenToolchainRequest.get().version.get())
        }
    }

    @Test
    fun `default version override does not configure the legacy binaryen setup`() {
        val project = wasmJsProject {
            propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_BINARYEN_DEFAULT_VERSION, "134")
        }

        val legacySpec = project.extensions.getByType(BinaryenEnvSpec::class.java)
        assertEquals(DEFAULT_BINARYEN_VERSION, legacySpec.version.get())
    }

    @Test
    fun `toolchains are disabled by default, so the legacy setup still provisions the tools`() {
        val project = wasmJsProject()

        assertIs<DisabledBinaryenToolchainServiceImpl>(project.registeredService(externalToolchainServiceName(BinaryenToolchainService::class.java)))

        assertTrue(
            project.tasks.names.contains("kotlinWasmD8Setup"),
            "Expected the legacy D8 setup task when the toolchain is disabled",
        )
        assertTrue(
            project.tasks.names.contains("kotlinWasmBinaryenSetup"),
            "Expected the legacy Binaryen setup task when the toolchain is disabled",
        )


    }

    @Test
    fun `enabling binaryen removes only its setup task and legacy executable`() {
        val project = wasmJsProject {
            propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_MODE, "download")
        }

        assertIs<DefaultBinaryenToolchainServiceImpl>(project.registeredService(externalToolchainServiceName(BinaryenToolchainService::class.java)))

        assertTrue(
            project.tasks.names.contains("kotlinWasmD8Setup"),
            "Expected the existing D8 setup task when Binaryen is enabled",
        )
        assertTrue(
            !project.tasks.names.contains("kotlinWasmBinaryenSetup"),
            "Expected no legacy Binaryen setup task when the toolchain is enabled",
        )

        project.tasks.withType(BinaryenExec::class.java).forEach { task ->
            assertNull(task.executable, "Expected ${task.name} to resolve Binaryen only at execution time")
            assertTrue(!task.legacyExecutable.isPresent, "Expected no legacy executable when the toolchain is enabled")
        }
    }

}

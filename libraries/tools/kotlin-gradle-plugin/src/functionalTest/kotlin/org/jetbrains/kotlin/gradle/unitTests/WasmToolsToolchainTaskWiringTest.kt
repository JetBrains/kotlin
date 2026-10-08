/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain.registerWasmToolsToolchainServiceIfAbsent
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain.requestDefaultWasmTools
import org.jetbrains.kotlin.gradle.tasks.wasmtools.UsesWasmToolsToolchainService
import org.jetbrains.kotlin.gradle.util.buildProject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@ExperimentalWasmDsl
class WasmToolsToolchainTaskWiringTest {

    internal abstract class TestWasmToolsTask : DefaultTask(), UsesWasmToolsToolchainService

    private fun buildProjectWithMode(mode: String? = null): Project = buildProject(configureProject = {
        if (mode != null) {
            extensions.extraProperties.set("kotlin.wasm.wasmtools.toolchain", mode)
        }
    })

    @Test
    fun `the toolchain service is set to every UsesWasmToolsToolchainService task`() {
        val project = buildProjectWithMode()
        val task = project.tasks.register("testWasmTools", TestWasmToolsTask::class.java)

        val service = registerWasmToolsToolchainServiceIfAbsent(project)

        assertEquals(service.get(), task.get().wasmToolsToolchainService.get())
    }

    @Test
    fun `no task depends on a wasm-tools setup task`() {
        val project = buildProjectWithMode()
        project.tasks.register("testWasmTools", TestWasmToolsTask::class.java)
        registerWasmToolsToolchainServiceIfAbsent(project)

        assertTrue(project.tasks.names.none { it.contains("wasmToolsSetup", ignoreCase = true) })
    }

    @Test
    fun `requesting wasm-tools from the disabled toolchain fails`() {
        val project = buildProjectWithMode("disable")

        val service = registerWasmToolsToolchainServiceIfAbsent(project).get()

        val failure = assertFailsWith<UnsupportedOperationException> {
            service.request(project.requestDefaultWasmTools().get())
        }
        assertEquals("wasm-tools toolchain is disabled", failure.message)
    }
}

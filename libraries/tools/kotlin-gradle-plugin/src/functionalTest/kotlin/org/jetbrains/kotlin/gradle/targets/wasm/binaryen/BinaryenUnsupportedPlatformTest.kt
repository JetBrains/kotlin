/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.binaryen

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinDiagnosticsException
import org.jetbrains.kotlin.gradle.util.assertContains
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.withModifiedSystemProperties
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@Isolated("Modifies system properties")
class BinaryenUnsupportedPlatformTest {

    @Test
    fun `unsupported platform fails with diagnostic when downloading`() {
        withUnsupportedOsArch {
            val project = buildWasmProject()

            val failure = assertFails {
                project.tasks.withType(BinaryenExec::class.java).toList()
            }

            val diagnosticException = generateSequence(failure) { it.cause }
                .filterIsInstance<KotlinDiagnosticsException>()
                .firstOrNull()
            assertNotNull(diagnosticException, "Expected KotlinDiagnosticsException, got:\n${failure.stackTraceToString()}")
            assertContains("Unsupported Binaryen Platform", diagnosticException.message ?: "<null>")
        }
    }

    @Test
    fun `unsupported platform does not fail when download is disabled`() {
        withUnsupportedOsArch {
            val project = buildWasmProject(download = false)

            val executables = project.tasks.withType(BinaryenExec::class.java)
                .map { it.executable }
                .toSet()

            // Without downloading, the configured command is used as-is
            assertEquals(setOf("wasm-opt"), executables)
        }
    }

    @Test
    fun `unsupported platform has no platform value`() {
        withUnsupportedOsArch {
            val project = buildWasmProject()

            val spec = project.extensions.getByType(BinaryenEnvSpec::class.java)
            assertFalse(spec.platform.isPresent, "Platform should be absent, but was ${spec.platform.orNull}")
        }
    }

    companion object {

        private fun buildWasmProject(download: Boolean = true): Project {
            val project = buildProjectWithMPP {
                plugins.withType(BinaryenPlugin::class.java) {
                    extensions.getByType(BinaryenEnvSpec::class.java).download.set(download)
                }
                multiplatformExtension.wasmJs {
                    nodejs()
                    binaries.executable()
                }
            }
            return project.evaluate()
        }

        private fun withUnsupportedOsArch(block: () -> Unit): Unit =
            // Binaryen does not publish Linux 32-bit x86 archives
            withModifiedSystemProperties(
                "os.name" to "linux",
                "os.arch" to "x86",
                block = block,
            )
    }
}

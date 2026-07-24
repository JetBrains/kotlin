/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimeEnvSpec
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WasmtimeEnvSpecTest {

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun checkChangeWasmtimeVersion() {
        val customVersion = "44.0.0"

        val project = buildProjectWithMPP {
            kotlin {
                wasmWasi {
                    wasmtime()
                }
            }

            val envSpec = extensions.getByType(WasmtimeEnvSpec::class.java)
            envSpec.version.set(customVersion)
        }.evaluate()

        assertEquals(
            customVersion,
            project.extensions.getByType(WasmtimeEnvSpec::class.java).version.orNull,
        )
    }

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun checkChangeWasmtimeDownload() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmWasi {
                    wasmtime()
                }
            }

            val envSpec = extensions.getByType(WasmtimeEnvSpec::class.java)

            envSpec.download.set(false)
        }.evaluate()

        assertEquals(
            false,
            project.extensions.getByType(WasmtimeEnvSpec::class.java).download.orNull,
        )
    }

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun checkChangeWasmtimeDownloadBaseUrl() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmWasi {
                    wasmtime()
                }
            }

            val envSpec = extensions.getByType(WasmtimeEnvSpec::class.java)

            envSpec.downloadBaseUrl.set(null as String?)
        }.evaluate()

        assertEquals(
            null,
            project.extensions.getByType(WasmtimeEnvSpec::class.java).downloadBaseUrl.orNull,
        )
    }

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun checkChangeWasmtimeAllowInsecureProtocol() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmWasi {
                    wasmtime()
                }
            }

            val envSpec = extensions.getByType(WasmtimeEnvSpec::class.java)

            envSpec.allowInsecureProtocol.set(true)
        }.evaluate()

        assertEquals(
            true,
            project.extensions.getByType(WasmtimeEnvSpec::class.java).allowInsecureProtocol.orNull,
        )
    }

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun checkChangeWasmtimeInstallationDirectory() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmWasi {
                    wasmtime()
                }
            }

            val envSpec = extensions.getByType(WasmtimeEnvSpec::class.java)

            envSpec.installationDirectory.set(project.rootDir)
        }.evaluate()

        assertEquals(
            project.rootDir.absolutePath,
            project.extensions.getByType(WasmtimeEnvSpec::class.java).installationDirectory.orNull?.asFile?.absolutePath,
        )
    }

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun checkChangeWasmtimeCommand() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmWasi {
                    wasmtime()
                }
            }

            val envSpec = extensions.getByType(WasmtimeEnvSpec::class.java)

            envSpec.command.set("test")
        }.evaluate()

        assertEquals(
            "test",
            project.extensions.getByType(WasmtimeEnvSpec::class.java).command.orNull,
        )
    }
}

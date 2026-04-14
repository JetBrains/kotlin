/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.targets.js.ir.DefaultIncrementalSyncTask
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinSimpleDevServerTask
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertTrue

class DevServerTaskTest {

    @Test
    fun `wasmJs browser target registers devServer task`() {
        val taskNames = buildProjectWithMPP {
            kotlin {
                @Suppress("OPT_IN_USAGE")
                wasmJs {
                    browser()
                    binaries.executable()
                }
            }
        }.evaluate().tasks.map { it.name }.toSet()

        assertTrue(
            taskNames.any { it.contains("devServer", ignoreCase = true) },
            "Expected devServer task to be registered for wasmJs browser target, but found tasks: $taskNames"
        )
    }

    @Test
    fun `wasmJs nodejs target does not register devServer task`() {
        val taskNames = buildProjectWithMPP {
            kotlin {
                @Suppress("OPT_IN_USAGE")
                wasmJs {
                    nodejs()
                    binaries.executable()
                }
            }
        }.evaluate().tasks.map { it.name }.toSet()

        assertTrue(
            taskNames.none { it.contains("devServer", ignoreCase = true) },
            "Expected no devServer task for wasmJs nodejs target, but found tasks: ${taskNames.filter { it.contains("devServer", ignoreCase = true) }}"
        )
    }

    @Test
    fun `wasmJs browser devServer task has import map configured`() {
        val project = buildProjectWithMPP {
            kotlin {
                @Suppress("OPT_IN_USAGE")
                wasmJs {
                    browser()
                    binaries.executable()
                }
            }
        }.evaluate()

        val devServerTask = project.tasks
            .filterIsInstance<KotlinSimpleDevServerTask>()
            .first()

        assertTrue(
            devServerTask.importMapFile.isPresent,
            "Expected importMapFile to be configured for wasmJs browser devServer task"
        )
        assertTrue(
            devServerTask.npmRootDirectory.isPresent,
            "Expected npmRootDirectory to be configured for wasmJs browser devServer task"
        )
    }

    @Test
    fun `wasmJs browser compileSync task includes import map files`() {
        val project = buildProjectWithMPP {
            kotlin {
                @Suppress("OPT_IN_USAGE")
                wasmJs {
                    browser()
                    binaries.executable()
                }
            }
        }.evaluate()

        val compileSyncTask = project.tasks
            .filterIsInstance<DefaultIncrementalSyncTask>()
            .first { it.name.contains("compileSync", ignoreCase = true) }

        val fromFiles = compileSyncTask.from.files.map { it.name }.toSet()

        assertTrue(
            fromFiles.any { it == "importmap-loader.js" },
            "Expected importmap-loader.js to be included in compileSync task inputs, but found: $fromFiles"
        )
    }
}

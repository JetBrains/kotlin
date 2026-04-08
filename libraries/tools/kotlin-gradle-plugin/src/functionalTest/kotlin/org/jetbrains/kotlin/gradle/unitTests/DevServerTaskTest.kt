/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinSimpleDevServerTask
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertFalse
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
    fun `js browser target registers devServer task`() {
        val taskNames = buildProjectWithMPP {
            kotlin {
                @Suppress("OPT_IN_USAGE")
                js {
                    browser()
                    binaries.executable()
                }
            }
        }.evaluate().tasks.map { it.name }.toSet()

        assertTrue(
            taskNames.any { it.contains("devServer", ignoreCase = true) },
            "Expected devServer task to be registered for js browser target, but found tasks: $taskNames"
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
    fun `js browser devServer task does not have import map configured`() {
        val project = buildProjectWithMPP {
            kotlin {
                @Suppress("OPT_IN_USAGE")
                js {
                    browser()
                    binaries.executable()
                }
            }
        }.evaluate()

        val devServerTask = project.tasks
            .filterIsInstance<KotlinSimpleDevServerTask>()
            .first()

        assertFalse(
            devServerTask.importMapFile.isPresent,
            "Expected importMapFile to NOT be configured for js browser devServer task"
        )
    }
}

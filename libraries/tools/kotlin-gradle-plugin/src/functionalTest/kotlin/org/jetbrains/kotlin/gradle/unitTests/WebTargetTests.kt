/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.util.assertDependsOn
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertTrue

class WebTargetTests {

    @Test
    fun `build task dependencies includes wasm specific tool tasks`() {
        val tasksInBuild = buildProjectWithMPP {
            kotlin {
                @Suppress("OPT_IN_USAGE")
                wasmJs {
                    nodejs()
                    d8()
                }
            }
        }.evaluate().tasks
            .map { it.name }

        val expected = setOf(
            "kotlinWasmNodeJsSetup",
            "kotlinWasmD8Setup",
            "kotlinWasmYarnSetup",
            "kotlinWasmBinaryenSetup",
            "wasmRootPackageJson",
            "kotlinWasmNpmInstall",
            "kotlinWasmToolingSetup",
        )
        assertTrue(
            tasksInBuild.toSet().containsAll(
                expected
            ),
            "No necessary Wasm specific tasks found in Task Graph: $tasksInBuild, expected: $expected"
        )
    }

    @Test
    fun `build task dependencies includes js specific tool tasks`() {
        val tasksInBuild = buildProjectWithMPP {
            kotlin {
                @Suppress("OPT_IN_USAGE")
                js {
                    nodejs()
                }
            }
        }.evaluate().tasks
            .map { it.name }

        val expected = setOf(
            "kotlinNodeJsSetup",
            "kotlinYarnSetup",
            "rootPackageJson",
            "kotlinNpmInstall",
        )
        assertTrue(
            tasksInBuild.toSet().containsAll(
                expected
            ),
            "No necessary JS specific tasks found in Task Graph: $tasksInBuild, expected: $expected"
        )
    }

    @Test
    fun `kotlinToolingSetup should depend on yarn setup in Yarn package manager project`() {
        val project = buildProjectWithMPP {
            kotlin {
                @Suppress("OPT_IN_USAGE")
                wasmJs {
                    nodejs()
                }
            }
        }.evaluate()

        val kotlinWasmToolingSetup = project.tasks.named("kotlinWasmToolingSetup").get()
        val kotlinWasmYarnSetup = project.tasks.named("kotlinWasmYarnSetup").get()
        kotlinWasmToolingSetup.assertDependsOn(kotlinWasmYarnSetup)

    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")
@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.io.File
import kotlin.test.assertEquals

class KotlinJsIrCompilationTest {

    @Test
    fun `fetch npmToolingDir - js - yarn`() {
        val project = buildProjectWithMPP()

        // must enable browser to ensure NodeJsRootPlugin is applied
        project.kotlin { js { browser() } }

        val compilation = project.multiplatformExtension.js().compilations.getByName(MAIN_COMPILATION_NAME)

        assertEquals(
            project.projectDir.resolve("build/js/packages/${project.name}"),
            compilation.npmToolingDir().orNull?.asFile,
        )
    }

    @Test
    fun `fetch npmToolingDir - js - npm`() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                project.projectDir.resolve("local.properties")
                    .appendText("kotlin.js.yarn=false\n")
            }
        )

        // must enable browser to ensure NodeJsRootPlugin is applied
        project.kotlin { js { browser() } }

        val compilation = project.multiplatformExtension.js().compilations.getByName(MAIN_COMPILATION_NAME)

        assertEquals(
            project.projectDir.resolve("build/js/packages/${project.name}"),
            compilation.npmToolingDir().orNull?.asFile,
        )
    }

    @Test
    fun `fetch npmToolingDir - wasmJs - yarn`() {
        val project = buildProjectWithMPP()

        project.kotlin { wasmJs { browser() } }

        val compilation = project.multiplatformExtension.wasmJs().compilations.getByName(MAIN_COMPILATION_NAME)

        val userHomeDir = File(System.getProperty("user.home"))

        assertEquals(
            userHomeDir.resolve(".kotlin/kotlin-npm-tooling/yarn"),
            compilation.npmToolingDir().orNull?.asFile?.parentFile,
        )
    }

    @Test
    fun `fetch npmToolingDir - wasmJs - npm`() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                project.projectDir.resolve("local.properties")
                    .appendText("kotlin.js.yarn=false\n")
            }
        )

        project.kotlin { wasmJs { browser() } }

        val compilation = project.multiplatformExtension.wasmJs().compilations.getByName(MAIN_COMPILATION_NAME)

        val userHomeDir = File(System.getProperty("user.home"))

        assertEquals(
            userHomeDir.resolve(".kotlin/kotlin-npm-tooling/npm"),
            compilation.npmToolingDir().orNull?.asFile?.parentFile,
        )
    }

    @Test
    fun `test hasSharedNpmToolingDir`() {
        val project = buildProjectWithMPP()
        project.kotlin {
            js { browser() }
            wasmJs { browser() }
            wasmWasi { nodejs() }
        }

        fun assertHasSharedNpmToolingDir(targetName: String, expected: Boolean) {
            val target = project.multiplatformExtension.targets.getByName<KotlinJsIrTarget>(targetName)
            val compilations = target.compilations
            assertAll(
                compilations.map { compilation ->
                    {
                        assertEquals(
                            expected = expected,
                            actual = compilation.hasSharedNpmToolingDir(),
                            message = "Expected target $targetName compilation ${compilation.name} ${if (expected) "has" else "does not have"} shared npm tooling dir",
                        )
                    }
                }
            )
        }

        assertHasSharedNpmToolingDir("wasmJs", true)
        assertHasSharedNpmToolingDir("js", false)
        assertHasSharedNpmToolingDir("wasmWasi", false)
    }
}

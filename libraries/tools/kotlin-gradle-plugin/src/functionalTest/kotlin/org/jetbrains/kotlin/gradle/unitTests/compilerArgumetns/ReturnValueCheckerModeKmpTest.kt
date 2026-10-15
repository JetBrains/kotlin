/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.ReturnValueCheckerMode
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test

class ReturnValueCheckerModeKmpTest : ReturnValueCheckerTestBase() {
    @Test
    fun `mode is unset by default`() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                setupTargets()
            }
        }
        project.evaluate()

        project.assertMainMode(null)
        project.assertTestMode(null)
    }

    @Test
    fun `test compilation inherits mode from main compilation`() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                setupTargets()
                returnValueCheckerMode.set(ReturnValueCheckerMode.Check)
            }
        }
        project.evaluate()

        project.assertMainMode(ReturnValueCheckerMode.Check)
        project.assertTestMode(ReturnValueCheckerMode.Check)
    }

    @Test
    fun `modes for main and test compilations can be set separately`() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                setupTargets()
                returnValueCheckerMode.set(ReturnValueCheckerMode.Full)
                returnValueCheckerModeForTests.set(ReturnValueCheckerMode.Check)
            }
        }
        project.evaluate()

        project.assertMainMode(ReturnValueCheckerMode.Full)
        project.assertTestMode(ReturnValueCheckerMode.Check)
    }

    @Test
    fun `setup mode for tests only`() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                setupTargets()
                returnValueCheckerModeForTests.set(ReturnValueCheckerMode.Disabled)
            }
        }
        project.evaluate()

        project.assertMainMode(null)
        project.assertTestMode(ReturnValueCheckerMode.Disabled)
    }

    private val mainCompilations = listOf(
        "compileCommonMainKotlinMetadata",
        "compileNativeMainKotlinMetadata",
        "compileKotlinJvm",
        "compileKotlinJs",
        "compileKotlinLinuxX64",
        "compileKotlinWasmJs",
    )

    private val testCompilations = listOf(
        "compileTestKotlinJvm",
        "compileTestKotlinJs",
        "compileTestKotlinLinuxX64",
        "compileTestKotlinWasmJs",
    )

    private fun ProjectInternal.assertMainMode(mode: ReturnValueCheckerMode?) {
        mainCompilations.forEach {
            assertMode(it, mode)
        }
    }

    private fun ProjectInternal.assertTestMode(mode: ReturnValueCheckerMode?) {
        testCompilations.forEach {
            assertMode(it, mode)
        }
    }

    private fun KotlinMultiplatformExtension.setupTargets() {
        jvm()
        js()
        wasmJs()
        // two native targets so that the shared 'nativeMain' (metadata) compilation is created
        linuxX64()
        linuxArm64()
    }
}

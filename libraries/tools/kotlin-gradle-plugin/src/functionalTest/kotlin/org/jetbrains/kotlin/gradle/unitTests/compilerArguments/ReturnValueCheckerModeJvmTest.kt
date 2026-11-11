/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.compilerArguments

import org.jetbrains.kotlin.gradle.dsl.ReturnValueCheckerMode
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.utils.named
import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnValueCheckerModeJvmTest : ReturnValueCheckerTestBase() {

    @Test
    fun `mode is not set by default`() {
        val project = buildProjectWithJvm()
        project.evaluate()

        project.assertMode("compileKotlin", null)
        project.assertMode("compileTestKotlin", null)
    }

    @Test
    fun `test compilation inherits mode from main compilation`() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.returnValueCheckerMode.set(ReturnValueCheckerMode.Check)
        project.evaluate()

        project.assertMode("compileKotlin", ReturnValueCheckerMode.Check)
        project.assertMode("compileTestKotlin", ReturnValueCheckerMode.Check)
    }

    @Test
    fun `modes for main and test compilations can be set separately`() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.apply {
            returnValueCheckerMode.set(ReturnValueCheckerMode.Full)
            returnValueCheckerModeForTests.set(ReturnValueCheckerMode.Check)
        }
        project.evaluate()

        project.assertMode("compileKotlin", ReturnValueCheckerMode.Full)
        project.assertMode("compileTestKotlin", ReturnValueCheckerMode.Check)
    }

    @Test
    fun `setup mode for tests only`() {
        // Only the test mode is configured; production is left unset.
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.returnValueCheckerModeForTests.set(ReturnValueCheckerMode.Check)
        project.evaluate()

        project.assertMode("compileKotlin", null)
        project.assertMode("compileTestKotlin", ReturnValueCheckerMode.Check)
    }

    @Test
    fun `setup mode for task`() {
        val project = buildProjectWithJvm()
        project.tasks.named<KotlinJvmCompile>("compileKotlin").configure {
            it.returnValueCheckerMode.set(ReturnValueCheckerMode.Full)
        }
        project.evaluate()

        project.assertMode("compileKotlin", ReturnValueCheckerMode.Full)
    }

    @Test // KT-90458
    fun `kapt stubs generation receives mode of main compilation`() {
        val project = buildProjectWithJvm {
            plugins.apply(Kapt3GradleSubplugin::class.java)
        }
        project.kotlinJvmExtension.returnValueCheckerMode.set(ReturnValueCheckerMode.Full)
        project.evaluate()

        project.assertMode("kaptGenerateStubsKotlin", ReturnValueCheckerMode.Full)
    }

    @Test // KT-90962
    fun `override cli arg`() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.apply {
            returnValueCheckerMode.set(ReturnValueCheckerMode.Full)
            compilerOptions {
                freeCompilerArgs.add("-Xreturn-value-checker=disable")
            }
        }
        project.evaluate()

        val lastArg = project.compileArguments("compileKotlin").last { it.contains("-Xreturn-value-checker=") }
        assertEquals("-Xreturn-value-checker=full", lastArg)
    }
}

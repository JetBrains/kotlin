/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.compilerRunner.KotlinCompilerArgumentsLogLevel
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCompilerRunner
import org.jetbrains.kotlin.compilerRunner.konanDataDir
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.named
import kotlin.test.Test
import kotlin.test.assertEquals

class CompilerArgumentsLogLevelTest {
    @Test
    fun checkTheDefaultLevel() {
        checkLogLevel(KotlinCompilerArgumentsLogLevel.DEFAULT)
    }

    @Test
    fun checkErrorLevel() {
        checkLogLevel(KotlinCompilerArgumentsLogLevel.ERROR, "error")
    }

    @Test
    fun checkWarnLevel() {
        checkLogLevel(KotlinCompilerArgumentsLogLevel.WARNING, "warning")
    }

    @Test
    fun checkInfoLevel() {
        checkLogLevel(KotlinCompilerArgumentsLogLevel.INFO, "info")
    }

    private fun checkLogLevel(
        expectedLogLevel: KotlinCompilerArgumentsLogLevel,
        levelToConfigure: String? = null
    ) {
        val project = buildProjectWithJvm(
            preApplyCode = {
                if (levelToConfigure != null) {
                    project.extraProperties.set(
                        "kotlin.internal.compiler.arguments.log.level",
                        levelToConfigure
                    )
                }
            }
        )

        project.evaluate()

        assertEquals(
            expectedLogLevel,
            project.tasks.named<KotlinCompile>("compileKotlin").get().kotlinCompilerArgumentsLogLevel.get()
        )
    }

    @Test
    fun `kotlin native compiler runner settings configures correctly with compiler arguments log level`() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                project.extraProperties.set(
                    "kotlin.internal.compiler.arguments.log.level",
                    "warning"
                )
            }
        ) {
            kotlin {
                linuxX64()
                linuxArm64()
            }
        }

        project.evaluate()

        val runnerSettings = KotlinNativeCompilerRunner.Settings.of(project.konanHome.absolutePath, project.konanDataDir, project)
        assertEquals(KotlinCompilerArgumentsLogLevel.WARNING, runnerSettings.parent.kotlinCompilerArgumentsLogLevel.get())
    }
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.main
import org.jetbrains.kotlin.gradle.util.test
import kotlin.test.*

class KotlinNativeCompilationModuleNameTest {

    @Test
    fun `main compilation module name convention`() {
        val project = buildProjectWithMPP(
            projectBuilder = {
                withName(PROJECT_NAME)
            }
        )
        val linuxX64Target = project.multiplatformExtension.linuxX64()

        project.evaluate()

        assertEquals(
            PROJECT_NAME,
            linuxX64Target.compilations.main.compilerOptions.options.moduleName.get(),
            "Main compilation moduleName value is not expected!"
        )
    }

    @Test
    fun `test compilation module name convention`() {
        val project = buildProjectWithMPP(
            projectBuilder = {
                withName(PROJECT_NAME)
            }
        )
        val linuxX64Target = project.multiplatformExtension.linuxX64()

        project.evaluate()

        assertEquals(
            "${PROJECT_NAME}_test",
            linuxX64Target.compilations.test.compilerOptions.options.moduleName.get(),
            "Test compilation moduleName value is not expected!"
        )
    }

    @Test
    fun `Metadata compilation module name convention`() {
        val project = buildProjectWithMPP(
            projectBuilder = {
                withName(PROJECT_NAME)
            }
        ) {
            with(multiplatformExtension) {
                targetHierarchy.default()
                linuxX64()
                linuxArm64()
            }
        }

        project.evaluate()

        val compilerOptions = project
            .multiplatformExtension
            .metadata()
            .compilations
            .getByName("linuxMain")
            .compilerOptions.options as KotlinNativeCompilerOptions
        assertEquals(
            "${PROJECT_NAME}_linuxMain",
            compilerOptions.moduleName.get(),
            "Main compilation moduleName value is not expected!"
        )
    }

    companion object {
        const val PROJECT_NAME = "nativeProject"
    }
}
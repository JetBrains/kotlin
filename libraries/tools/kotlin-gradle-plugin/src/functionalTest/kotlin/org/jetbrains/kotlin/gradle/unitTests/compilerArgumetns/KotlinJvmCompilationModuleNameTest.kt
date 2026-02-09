/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.main
import org.jetbrains.kotlin.gradle.util.test
import kotlin.test.*

class KotlinJvmCompilationModuleNameTest {

    @Test
    fun testJVMProjectWithGroup() {
        val project = buildProjectWithJvm(
            projectBuilder = {
                withName(PROJECT_NAME)
            },
            preApplyCode = {
                group = GROUP_ID
            }
        )

        project.evaluate()

        assertEquals(
            "$GROUP_ID:$PROJECT_NAME",
            project.kotlinJvmExtension.compilerOptions.moduleName.get(),
            "JVM project module name should include group prefix in extension options",
        )

        val jvmMainCompilationTaskCompilerOptions = project.kotlinJvmExtension.target.compilations.main.compileTaskProvider
            .map { it.compilerOptions as KotlinJvmCompilerOptions }
        assertEquals(
            "$GROUP_ID:$PROJECT_NAME",
            jvmMainCompilationTaskCompilerOptions.get().moduleName.get(),
            "JVM project module name should include group prefix in task options",
        )

        val jvmTestCompilationTaskCompilerOptions = project.kotlinJvmExtension.target.compilations.test.compileTaskProvider
            .map { it.compilerOptions as KotlinJvmCompilerOptions }
        assertEquals(
            "$GROUP_ID:${PROJECT_NAME}_test",
            jvmTestCompilationTaskCompilerOptions.get().moduleName.get(),
            "JVM project module name should include group prefix with _test suffix in task options",
        )
    }

    @Test
    fun testJVMProjectWithoutGroup() {
        val project = buildProjectWithJvm(
            projectBuilder = {
                withName(PROJECT_NAME)
            }
        )

        project.evaluate()

        // In single module projects default group name is equal to path which is empty
        assertEquals(
            PROJECT_NAME,
            project.kotlinJvmExtension.compilerOptions.moduleName.get(),
            "JVM project without group should use plain project name in extension options",
        )

        val jvmMainCompilationTaskCompilerOptions = project.kotlinJvmExtension.target.compilations.main.compileTaskProvider
            .map { it.compilerOptions as KotlinJvmCompilerOptions }
        assertEquals(
            PROJECT_NAME,
            jvmMainCompilationTaskCompilerOptions.get().moduleName.get(),
            "JVM project without group should use plain project name in task options",
        )

        val jvmTestCompilationTaskCompilerOptions = project.kotlinJvmExtension.target.compilations.test.compileTaskProvider
            .map { it.compilerOptions as KotlinJvmCompilerOptions }
        assertEquals(
            "${PROJECT_NAME}_test",
            jvmTestCompilationTaskCompilerOptions.get().moduleName.get(),
            "JVM project without group should use plain project name with _test suffix in test task options",
        )
    }

    @Test
    fun testKmpJVMTargetWithGroupMainCompilation() {
        val project = buildProjectWithMPP(
            projectBuilder = {
                withName(PROJECT_NAME)
            },
            preApplyCode = {
                group = GROUP_ID
            }
        )
        val jvmTarget = project.multiplatformExtension.jvm()

        project.evaluate()

        assertEquals(
            "$GROUP_ID:$PROJECT_NAME",
            jvmTarget.compilerOptions.moduleName.get(),
            "KMP JVM target should include group prefix in options"
        )

        assertEquals(
            "$GROUP_ID:$PROJECT_NAME",
            jvmTarget.compilations.main.compileTaskProvider.get().compilerOptions.moduleName.get(),
            "KMP JVM main compilation should include group prefix in task options"
        )
    }

    @Test
    fun testKmpJVMTargetWithGroupTestCompilation() {
        val project = buildProjectWithMPP(
            projectBuilder = {
                withName(PROJECT_NAME)
            },
            preApplyCode = {
                group = GROUP_ID
            }
        )
        val jvmTarget = project.multiplatformExtension.jvm()

        project.evaluate()

        assertEquals(
            "$GROUP_ID:${PROJECT_NAME}_test",
            jvmTarget.compilations.test.compileTaskProvider.get().compilerOptions.moduleName.get(),
            "KMP JVM test compilation should include group prefix with _test suffix in task options"
        )
    }

    @Test
    fun testKmpJVMTargetWithoutGroupMainCompilation() {
        val project = buildProjectWithMPP(
            projectBuilder = {
                withName(PROJECT_NAME)
            }
        )
        val jvmTarget = project.multiplatformExtension.jvm()

        project.evaluate()

        assertEquals(
            PROJECT_NAME,
            jvmTarget.compilerOptions.moduleName.get(),
            "Multiplatform JVM target without group should use plain project name"
        )

        assertEquals(
            PROJECT_NAME,
            jvmTarget.compilations.main.compileTaskProvider.get().compilerOptions.moduleName.get(),
            "Multiplatform JVM compilation without group should use plain project name"
        )
    }

    @Test
    fun testKmpJVMTargetWithoutGroupTestCompilation() {
        val project = buildProjectWithMPP(
            projectBuilder = {
                withName(PROJECT_NAME)
            }
        )
        val jvmTarget = project.multiplatformExtension.jvm()

        project.evaluate()

        assertEquals(
            "${PROJECT_NAME}_test",
            jvmTarget.compilations.test.compileTaskProvider.get().compilerOptions.moduleName.get(),
            "Multiplatform JVM test compilation without group should use plain project name with _test suffix"
        )
    }


    companion object {
        const val PROJECT_NAME = "jvmProject"
        const val GROUP_ID = "com.example"
    }
}

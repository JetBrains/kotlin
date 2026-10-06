/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.main
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinCommonCompilationModuleNameTest {

    @Test
    fun testKmpProjectWithGroup() {
        val project = buildProjectWithMPP(
            projectBuilder = { withName(PROJECT_NAME) }
        ) {
            group = GROUP_ID

            kotlin {
                jvm()
            }
        }

        project.evaluate()

        val metadataTask = project.multiplatformExtension.metadataTarget.compilations
            .main
            .compileTaskProvider.get() as KotlinCompileCommon
        @Suppress("DEPRECATION")
        assertEquals(
            "$GROUP_ID:$PROJECT_NAME",
            metadataTask.moduleName.get()
        )
    }

    @Test
    fun testKmpProjectWithoutGroup() {
        val project = buildProjectWithMPP(
            projectBuilder = { withName(PROJECT_NAME) }
        ) {
            kotlin {
                jvm()
            }
        }

        val metadataTask = project.multiplatformExtension.metadataTarget.compilations
            .main
            .compileTaskProvider.get() as KotlinCompileCommon
        @Suppress("DEPRECATION")
        assertEquals(
            PROJECT_NAME,
            metadataTask.moduleName.get()
        )
    }

    companion object {
        const val GROUP_ID = "com.example"
        const val PROJECT_NAME = "test"
    }
}

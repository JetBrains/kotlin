/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.jvm

import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.junit.Test
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.utils.withType
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmFirIncrementalCompilationTest {

    @Test
    fun byDefaultFirIcShouldBeDisabled() {
        val project = buildProjectWithJvm()

        project.evaluate()

        val kotlinJvmCompileTasks = project.tasks.withType<KotlinCompile>()
        kotlinJvmCompileTasks.all { task ->
            assertFalse(
                actual = task.useFirRunner.get(),
                message = "Task ${task.path} has configured 'true' for 'useFirRunner'"
            )
        }
    }

    @Test
    fun enabledFirIcShouldBeConfiguredCorrectly() {
        val project = buildProjectWithJvm(preApplyCode = {
            extraProperties.set("kotlin.incremental.jvm.fir", "true")
        })

        project.evaluate()

        val kotlinJvmCompileTasks = project.tasks.withType<KotlinCompile>()
        kotlinJvmCompileTasks.all { task ->
            assertTrue(
                actual = task.useFirRunner.get(),
                message = "Task ${task.path} has configured 'false' for 'useFirRunner'"
            )
        }
    }
}
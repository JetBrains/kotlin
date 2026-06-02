/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.jvm

import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.utils.withType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class JvmClasspathMetadataTest {

    @Test
    fun byDefaultJvmClasspathMetadataShouldBeDisabled() {
        val project = buildProjectWithJvm()

        project.evaluate()

        val kotlinJvmCompileTasks = project.tasks.withType<KotlinCompile>()
        kotlinJvmCompileTasks.all { task ->
            assertFalse(
                actual = task.enableJvmClasspathMetadata.get(),
                message = "Task ${task.path} has configured 'true' for 'enableKmpJvmClasspathMetadata'"
            )
        }
    }

    @Test
    fun enabledJvmClasspathMetadataShouldBeConfiguredCorrectly() {
        val project = buildProjectWithJvm(preApplyCode = {
            extraProperties.set("kotlin.internal.kmp.jvmClasspathMetadata", "true")
        })

        project.evaluate()

        val kotlinJvmCompileTasks = project.tasks.withType<KotlinCompile>()
        kotlinJvmCompileTasks.all { task ->
            assertTrue(
                actual = task.enableJvmClasspathMetadata.get(),
                message = "Task ${task.path} has configured 'false' for 'enableKmpJvmClasspathMetadata'"
            )
        }
    }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test intended to check if all relevant [AbstractArchiveTask]'s are configured correctly, to ensure
 * reproducible binary artifacts between builds.
 */
class ArchiveReproducibilityTest {

    @Test
    fun `test simple multiplatform project`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                js { browser() }
                linuxX64()
                linuxArm64()

                val commonMain = sourceSets.getByName("commonMain")
                val linuxX64Main = sourceSets.getByName("linuxX64Main")
                val linuxArm64Main = sourceSets.getByName("linuxArm64Main")
                val nativeMain = sourceSets.create("nativeMain")

                nativeMain.dependsOn(commonMain)
                linuxX64Main.dependsOn(nativeMain)
                linuxArm64Main.dependsOn(nativeMain)
            }
        }

        project.evaluate()
        project.assertAllArchiveTasksAreReproducible()
    }

    private fun Project.assertAllArchiveTasksAreReproducible() {
        tasks.withType(AbstractArchiveTask::class.java).forEach { task ->
            /* Check that we keep file order in zip files, to get the same binary */
            assertTrue(task.isReproducibleFileOrder, "Expected ${task.path} to set 'isReproducibleFileOrder'")

            /* Check that zip files do not include the actual timestamps (everything will be set to the same date) */
            assertFalse(task.isPreserveFileTimestamps, "Expected ${task.path} to *not* set 'isPreserveFileTimestamps'")
        }
    }
}

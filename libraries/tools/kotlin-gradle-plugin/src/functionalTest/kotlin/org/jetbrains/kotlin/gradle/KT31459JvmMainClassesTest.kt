/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Task
import kotlin.test.Test
import kotlin.test.assertEquals

class KT31459JvmMainClassesTest {

    @Test
    fun `jvmMainClasses should depend on compileJava`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm {
                    withJava()
                }
            }
        }

        project.evaluate()

        val task = project.tasks.getByName("jvmMainClasses")

        assertEquals(
            setOf("compileKotlinJvm", "compileJava", "jvmProcessResources"),
            task.allDependencies
        )
    }

    /**
     * Returns names of all tasks that given task depends on
     */
    private val Task.allDependencies: Set<String> get() = taskDependencies
        .getDependencies(this)
        .map { it.name }
        .toSet()
}
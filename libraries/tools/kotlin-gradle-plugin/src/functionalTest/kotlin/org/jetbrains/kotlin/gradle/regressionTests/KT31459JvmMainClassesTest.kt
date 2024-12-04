/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.util.assertNoCircularTaskDependencies
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
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
            task.directDependencies
        )
    }

    /**
     * This mechanism used by `kotlinx-atomicfu` gradle plugin. It replaces compilation classes to transformed classes dir
     */
    @Test
    fun `it should be possible to replace compilation output classes`() {

        /**
         * Code taken from `kotlinx-atomicfu` gradle plugin
         */
        fun Project.addClassesTransformationTask(target: KotlinTarget) {
            val compilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            val classesDirs = compilation.output.classesDirs

            // make copy of original classes directory
            val originalClassesDirs: FileCollection =
                project.files(classesDirs.from.toTypedArray())
            val transformedClassesDir = project.layout.buildDirectory.dir("classes/atomicfu/${target.name}/${compilation.name}")
            val transformTask = project.tasks.create("transformTask") {
                it.dependsOn(compilation.compileAllTaskName)
                it.inputs.files(originalClassesDirs)
                it.outputs.files(transformedClassesDir)
            }
            classesDirs.setFrom(transformedClassesDir)
            classesDirs.builtBy(transformTask)
        }

        // Given MPP Project with JVM target
        val project = buildProjectWithMPP {
            kotlin {
                val target = jvm {}

                // And classes transformation task applied
                addClassesTransformationTask(target)
            }
        }

        project.evaluate()

        val jvmMainClasses = project.tasks.getByName("jvmMainClasses")
        jvmMainClasses.assertNoCircularTaskDependencies()
    }

    /**
     * Returns names of all tasks that given task directly depends on
     */
    private val Task.directDependencies: Set<String>
        get() = taskDependencies
            .getDependencies(null)
            .map { it.name }
            .toSet()
}
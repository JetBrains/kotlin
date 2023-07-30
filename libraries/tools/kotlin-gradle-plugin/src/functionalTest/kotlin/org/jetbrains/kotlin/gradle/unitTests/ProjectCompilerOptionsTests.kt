/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.utils.named
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectCompilerOptionsTests {

    @Test
    fun nativeTargetCompilerOptionsDSL() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                linuxX64 {
                    compilerOptions {
                        progressiveMode.set(true)
                    }
                }

                iosX64 {
                    compilerOptions {
                        suppressWarnings.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        assertEquals(true, project.kotlinNativeTask("compileKotlinLinuxX64").compilerOptions.progressiveMode.get())
        assertEquals(false, project.kotlinNativeTask("compileKotlinLinuxX64").compilerOptions.suppressWarnings.get())
        assertEquals(true, project.kotlinNativeTask("compileTestKotlinLinuxX64").compilerOptions.progressiveMode.get())
        assertEquals(false, project.kotlinNativeTask("compileTestKotlinLinuxX64").compilerOptions.suppressWarnings.get())
        assertEquals(true, project.kotlinNativeTask("compileKotlinIosX64").compilerOptions.suppressWarnings.get())
        assertEquals(false, project.kotlinNativeTask("compileKotlinIosX64").compilerOptions.progressiveMode.get())
        assertEquals(true, project.kotlinNativeTask("compileTestKotlinIosX64").compilerOptions.suppressWarnings.get())
        assertEquals(false, project.kotlinNativeTask("compileTestKotlinIosX64").compilerOptions.progressiveMode.get())
    }

    @Test
    fun nativeTaskOverridesTargetOptions() {
        val project = buildProjectWithMPP {
            tasks.withType<KotlinCompilationTask<*>>().configureEach {
                if (it.name == "compileKotlinLinuxX64") {
                    it.compilerOptions.progressiveMode.set(false)
                }
            }

            with(multiplatformExtension) {
                linuxX64 {
                    compilerOptions {
                        progressiveMode.set(true)
                    }
                }

                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        assertEquals(false, project.kotlinNativeTask("compileKotlinLinuxX64").compilerOptions.progressiveMode.get())
    }

    private fun Project.kotlinNativeTask(name: String): KotlinCompilationTask<KotlinNativeCompilerOptions> = tasks
        .named<KotlinCompilationTask<KotlinNativeCompilerOptions>>(name)
        .get()
}
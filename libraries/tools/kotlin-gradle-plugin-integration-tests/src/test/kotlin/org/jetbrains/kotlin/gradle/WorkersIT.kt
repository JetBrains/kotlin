/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.plugin.JsMode
import org.jetbrains.kotlin.gradle.util.jsMode
import org.junit.Test

class WorkersIT : BaseGradleIT() {
    @Test
    fun testParallelTasks() {
        parallelTasksImpl(
            isParallel = true,
            jsIr = true
        )
        parallelTasksImpl(
            isParallel = true,
            jsIr = false
        )
    }

    @Test
    fun testNoParallelTasks() {
        parallelTasksImpl(
            isParallel = false,
            jsIr = true
        )
        parallelTasksImpl(
            isParallel = false,
            jsIr = false
        )
    }

    private fun parallelTasksImpl(
        isParallel: Boolean,
        jsIr: Boolean
    ) =
        with(Project("new-mpp-parallel")) {
            val options = defaultBuildOptions().copy(parallelTasksInProject = isParallel, withDaemon = false)
            val traceLoading = "-Dorg.jetbrains.kotlin.compilerRunner.GradleKotlinCompilerWork.trace.loading=true"
            if (!jsIr) {
                gradleProperties().appendText(jsMode(JsMode.LEGACY))
            }
            build("assemble", traceLoading, options = options) {
                assertSuccessful()
                val tasks = arrayOf(":compileKotlinMetadata", ":compileKotlinJvm", ":compileKotlinJs")
                if (isParallel) {
                    assertTasksSubmittedWork(*tasks)
                } else {
                    assertTasksDidNotSubmitWork(*tasks)
                }
                val expectedKotlinOutputFiles = listOf(
                    kotlinClassesDir(sourceSet = "metadata/main") + "common/A.kotlin_metadata",
                    kotlinClassesDir(sourceSet = "jvm/main") + "common/A.class",
                    kotlinClassesDir(sourceSet = "js/main") +
                            if (jsIr) "manifest" else "new-mpp-parallel.js"
                )
                expectedKotlinOutputFiles.forEach { assertFileExists(it) }
                assertSubstringCount("Loaded GradleKotlinCompilerWork", 1)
                assertCompiledKotlinSources(project.relativize(project.allKotlinFiles))
                assertNotContains("Falling back to sl4j logger")
            }
        }
}
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
            jsMode = JsMode.LEGACY
        )
    }

    @Test
    fun testParallelTasksJsIr() {
        parallelTasksImpl(
            isParallel = true,
            jsMode = JsMode.IR
        )
    }

    @Test
    fun testNoParallelTasks() {
        parallelTasksImpl(
            isParallel = false,
            jsMode = JsMode.LEGACY
        )
    }

    @Test
    fun testNoParallelTasksJsIr() {
        parallelTasksImpl(
            isParallel = false,
            jsMode = JsMode.IR
        )
    }

    private fun parallelTasksImpl(
        isParallel: Boolean,
        jsMode: JsMode
    ) =
        with(Project("new-mpp-parallel")) {
            val options = defaultBuildOptions().copy(
                parallelTasksInProject = isParallel,
                withDaemon = false,
                jsMode = jsMode
            )
            val traceLoading = "-Dorg.jetbrains.kotlin.compilerRunner.GradleKotlinCompilerWork.trace.loading=true"
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
                            if (jsMode == JsMode.IR) "manifest" else "new-mpp-parallel.js"
                )
                expectedKotlinOutputFiles.forEach { assertFileExists(it) }
                assertSubstringCount("Loaded GradleKotlinCompilerWork", 1)
                assertCompiledKotlinSources(project.relativize(project.allKotlinFiles))
                assertNotContains("Falling back to sl4j logger")
            }
        }
}
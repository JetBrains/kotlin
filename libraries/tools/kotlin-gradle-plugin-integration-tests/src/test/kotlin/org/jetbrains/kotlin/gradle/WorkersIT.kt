/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.junit.Test

class WorkersIT : BaseGradleIT() {
    @Test
    fun testParallelTasks() {
        parallelTasksImpl(isParallel = true)
    }

    @Test
    fun testNoParallelTasks() {
        parallelTasksImpl(isParallel = false)
    }

    private fun parallelTasksImpl(isParallel: Boolean) =
        with(Project("new-mpp-parallel", GradleVersionRequired.AtLeast("4.7"))) {
            val options = defaultBuildOptions().copy(parallelTasksInProject = isParallel, withDaemon = false)
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
                    kotlinClassesDir(sourceSet = "js/main") + "new-mpp-parallel.js"
                )
                expectedKotlinOutputFiles.forEach { assertFileExists(it) }
                assertSubstringCount("Loaded GradleKotlinCompilerWork", 1)
                assertCompiledKotlinSources(project.relativize(project.allKotlinFiles))
                assertNotContains("Falling back to sl4j logger")
            }
        }
}
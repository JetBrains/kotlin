/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.junit.Test

class WorkersIT : BaseGradleIT() {
    @Test
    fun testParallelTasks() {
        parallelTasksImpl(
            isParallel = true,
            jsCompilerType = KotlinJsCompilerType.legacy
        )
    }

    @Test
    fun testParallelTasksJsIr() {
        parallelTasksImpl(
            isParallel = true,
            jsCompilerType = KotlinJsCompilerType.ir
        )
    }

    @Test
    fun testNoParallelTasks() {
        parallelTasksImpl(
            isParallel = false,
            jsCompilerType = KotlinJsCompilerType.legacy
        )
    }

    @Test
    fun testNoParallelTasksJsIr() {
        parallelTasksImpl(
            isParallel = false,
            jsCompilerType = KotlinJsCompilerType.ir
        )
    }

    private fun parallelTasksImpl(
        isParallel: Boolean,
        jsCompilerType: KotlinJsCompilerType
    ) =
        with(Project("new-mpp-parallel")) {
            val options = defaultBuildOptions().copy(
                parallelTasksInProject = isParallel,
                withDaemon = false,
                jsCompilerType = jsCompilerType
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
                            if (jsCompilerType == KotlinJsCompilerType.ir) "default/manifest" else "new-mpp-parallel.js"
                )
                expectedKotlinOutputFiles.forEach { assertFileExists(it) }
                assertSubstringCount("Loaded GradleKotlinCompilerWork", 1)
                assertCompiledKotlinSources(project.relativize(project.allKotlinFiles))
                assertNotContains("Falling back to sl4j logger")
            }
        }
}
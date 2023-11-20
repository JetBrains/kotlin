/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.relativizeTo
import java.nio.file.Path
import kotlin.io.path.appendText

abstract class KmpIncrementalITBase : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK2()

    protected open val gradleTask = "assemble"
    protected open val projectName = "generic-kmp-app-plus-lib-with-tests"
    protected open val mainCompileTasks = setOf(
        ":app:compileKotlinJvm",
        ":app:compileKotlinJs",
        ":app:compileKotlinNative",
        ":lib:compileKotlinJvm",
        ":lib:compileKotlinJs",
        ":lib:compileKotlinNative",
    )

    protected open fun withProject(gradleVersion: GradleVersion, test: TestProject.() -> Unit): Unit {
        nativeProject(
            projectName,
            gradleVersion,
            configureSubProjects = true,
            test = test
        )
    }

    protected open fun TestProject.touchAndGet(subproject: String, srcDir: String, name: String): Path {
        val path = subProject(subproject).kotlinSourcesDir(srcDir).resolve(name)
        path.appendText("private val nothingMuch = 24")
        return path
    }

    protected open fun BuildResult.assertSuccessOrUTD(allTasks: Set<String>, executedTasks: Set<String>) {
        assertTasksExecuted(executedTasks)
        assertTasksUpToDate(allTasks - executedTasks)
    }

    protected open fun TestProject.testCase(incrementalPath: Path? = null, executedTasks: Set<String>, assertions: BuildResult.() -> Unit = {}) {
        build(gradleTask, buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
            assertSuccessOrUTD(
                allTasks = mainCompileTasks,
                executedTasks = executedTasks
            )
            incrementalPath?.let {
                assertIncrementalCompilation(listOf(it).relativizeTo(projectPath))
            }
            assertions()
        }
    }
}
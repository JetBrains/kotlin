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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.appendText

/**
 * KmpIncrementalITBase is used as a "limited scope" for experimental test utils.
 * If some of them can be reused widely, consider moving them to KGPBaseTest
 * or the appropriate util package. //TODO: KT-63876
 */
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

    protected fun TestProject.resolvePath(subproject: String, srcDir: String, name: String): Path {
        return subProject(subproject).kotlinSourcesDir(srcDir).resolve(name)
    }

    protected fun Path.addPrivateVal(): Path {
        this@addPrivateVal.appendText("private val nothingMuch${changeCounter.incrementAndGet()} = 24")
        return this@addPrivateVal
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

    companion object {
        private val changeCounter = AtomicInteger(0)
    }
}
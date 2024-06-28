/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceWithVersion
import java.nio.file.Path

/**
 * Convenience class for KMP incremental compilation tests.
 *
 * Consider moving all general-purpose logic to `org.jetbrains.kotlin.gradle.testbase` package.
 */
abstract class KmpIncrementalITBase : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK2().copy(
            /**
             * disable IC-breaking feature; it's tested separately in [org.jetbrains.kotlin.gradle.mpp.CommonCodeWithPlatformSymbolsITBase]
             */
            enableUnsafeIncrementalCompilationForMultiplatform = true
        )

    protected open val gradleTask = "assemble"
    protected open val projectName = "generic-kmp-app-plus-lib-with-tests"
    protected open val mainCompileTasks = setOf(
        ":app:compileCommonMainKotlinMetadata",
        ":app:compileKotlinJvm",
        ":app:compileKotlinJs",
        ":app:compileKotlinNative",
        ":lib:compileCommonMainKotlinMetadata",
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

    protected open fun TestProject.checkIncrementalBuild(
        tasksExpectedToExecute: Set<String>,
        assertions: BuildResult.() -> Unit = {}
    ) {
        build(gradleTask, buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
            assertTasksExecuted(tasksExpectedToExecute)
            assertTasksUpToDate(mainCompileTasks - tasksExpectedToExecute)
            assertions()
        }
    }

    protected open fun TestProject.multiStepCheckIncrementalBuilds(
        incrementalPath: Path,
        steps: List<String>,
        tasksExpectedToExecuteOnEachStep: Set<String>,
        afterEachStep: BuildResult.() -> Unit = {}
    ) {
        for (step in steps) {
            incrementalPath.replaceWithVersion(step)
            build(gradleTask, buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksExecuted(tasksExpectedToExecuteOnEachStep)
                assertTasksUpToDate(mainCompileTasks - tasksExpectedToExecuteOnEachStep)
                afterEachStep()
            }
        }
    }
}

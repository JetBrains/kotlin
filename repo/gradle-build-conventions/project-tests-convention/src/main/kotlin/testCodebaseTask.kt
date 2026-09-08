/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Task
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test

private val CODEBASE_CHECKING_TASK_NAMES = setOf(
    "updateKotlinAbi",
    "checkKotlinAbi",
)

/**
 * Prefixes of the names of tasks that cannot be listed exhaustively: `registerForeignClassUsageTasks` appends a
 * per-dump suffix, so a module tracking more than one dump names its tasks as it sees fit.
 */
private val CODEBASE_CHECKING_TASK_NAME_PREFIXES = setOf(
    "checkForeignClassUsage",
    "updateForeignClassUsage",
)

/**
 * Registers a JUnit 5 test task that runs tests from the `codebaseTest` source set.
 *
 * Codebase tests validate the project's source code itself (e.g., API surface checks,
 * documentation requirements, coding conventions). They depend on `src/` and [dumpDirs] directories as inputs.
 *
 * The `codebaseTest` source set is created automatically and inherits
 * `testCompileClasspath`/`testRuntimeClasspath` dependencies.
 * The task is added to the `check` lifecycle.
 *
 * Usage:
 * ```kotlin
 * projectTests {
 *     testCodebaseTask()
 * }
 * ```
 */
fun ProjectTestsExtension.testCodebaseTask(
    taskName: String = "testCodebase",
    dumpDirs: List<String> = listOf("api"),
    sourceSetName: String = "codebaseTest",
    body: Test.() -> Unit = {},
): TaskProvider<out Task> {
    val codebaseTest = project.sourceSets.maybeCreate(sourceSetName).apply {
        projectDefault(project)
    }

    return testTask(
        taskName = taskName,
        skipInLocalBuild = false,
    ) {
        group = "verification"
        classpath += codebaseTest.runtimeClasspath
        testClassesDirs = codebaseTest.output.classesDirs

        val projectDirectory = project.isolated.projectDirectory
        inputs.dir(projectDirectory.dir("src"))
            .withPropertyName("srcDir")
            .withPathSensitivity(PathSensitivity.RELATIVE)

        for (dumpDir in dumpDirs) {
            inputs.dir(projectDirectory.dir(dumpDir))
                .withPropertyName("dumpDir-$dumpDir")
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }

        // Since the codebase task shares files with other tasks, we need to run it after all other tasks.
        // Some modules may not use all checks, so only the names this one registered are matched.
        val tasksToRunBefore = project.tasks.names.filter { taskName ->
            taskName in CODEBASE_CHECKING_TASK_NAMES ||
                    CODEBASE_CHECKING_TASK_NAME_PREFIXES.any { prefix -> taskName.startsWith(prefix) }
        }

        for (taskToRunBefore in tasksToRunBefore) {
            mustRunAfter(project.tasks.named(taskToRunBefore))
        }

        body()
    }.also { testCodebaseProvider ->
        project.tasks.named("check") {
            dependsOn(testCodebaseProvider)
        }

        // Part of the project's API surface: the codebase tests hold the conventions its declarations follow and
        // compare the committed dumps under 'dumpDirs' against the sources. There is nothing to rewrite here — the
        // dumps are maintained by hand — so the same task answers for both halves of the surface lifecycle.
        for (apiSurfaceTaskName in listOf("checkApiSurface", "updateApiSurface")) {
            project.tasks.named(apiSurfaceTaskName) {
                dependsOn(testCodebaseProvider)
            }
        }
    }
}

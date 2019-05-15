/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.TaskHolder
import org.jetbrains.kotlin.gradle.tasks.AggregateTestReport
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask

private val Project.allTestsTask: TaskHolder<AggregateTestReport>
    get() = getAggregatedTestTask(
        name = "allTests",
        description = "Runs the tests for all targets and create aggregated report",
        reportName = "all"
    ).also {
        tasks.maybeCreate(LifecycleBasePlugin.CHECK_TASK_NAME)
            .dependsOn(it.getTaskOrProvider())
    }

internal fun Project.getAggregatedTestTask(name: String, description: String, reportName: String): TaskHolder<AggregateTestReport> {
    return locateOrRegisterTask(name) { aggregate ->
        aggregate.description = description
        aggregate.group = JavaBasePlugin.VERIFICATION_GROUP

        aggregate.reports.configureConventions(project, reportName)

        aggregate.onlyIf {
            aggregate.testTasks.size > 1
        }

        if (System.getProperty("idea.active") != null) {
            aggregate.extensions.extraProperties.set("idea.internal.test", true)
        }
    }
}

private fun cleanTaskName(taskName: String): String {
    check(taskName.isNotEmpty())
    return "clean" + taskName.capitalize()
}

private val Project.cleanAllTestTask: Task
    get() = tasks.findByName(cleanTaskName(allTestsTask.name))
        ?: tasks.create("clean", Task::class.java).also {
            tasks.getByName(LifecycleBasePlugin.CLEAN_TASK_NAME).dependsOn(it)
        }

@Suppress("UnstableApiUsage")
internal fun registerTestTask(taskHolder: TaskHolder<AbstractTestTask>) {
    registerTestTaskInAggregate(taskHolder, taskHolder.project.allTestsTask.doGetTask())
}

internal fun registerTestTaskInAggregate(
    taskHolder: TaskHolder<AbstractTestTask>,
    allTests: AggregateTestReport
) {
    val project = taskHolder.project

    project.tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(taskHolder.name)
    project.cleanAllTestTask.dependsOn(cleanTaskName(taskHolder.name))
    allTests.dependsOn(taskHolder.name)

    taskHolder.configure { task ->
        allTests.registerTestTask(task)

        project.gradle.taskGraph.whenReady {
            if (it.hasTask(allTests)) {
                // when [allTestsTask] task enabled, test failure should be reported only on [allTestsTask],
                // not at individual target's test tasks. To do that, we need:
                // - disable all reporting in test tasks
                // - enable [checkFailedTests] on [allTestsTask]

                task.ignoreFailures = true

                @Suppress("UnstableApiUsage")
                task.reports.html.isEnabled = false

                @Suppress("UnstableApiUsage")
                task.reports.junitXml.isEnabled = false

                allTests.checkFailedTests = true
                allTests.ignoreFailures = false
            }
        }

        ijListenTestTask(task)
    }
}
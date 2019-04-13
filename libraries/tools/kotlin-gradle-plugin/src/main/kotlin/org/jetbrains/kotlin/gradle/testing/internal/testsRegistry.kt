/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.internal

import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.TaskHolder
import org.jetbrains.kotlin.gradle.tasks.AggregateTestReport
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask

private val Project.allTestsTask: TaskHolder<AggregateTestReport>
    get() = locateOrRegisterTask("allTests") { aggregate ->
        tasks.maybeCreate(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(aggregate)

        aggregate.description = "Runs the tests for all targets and create aggregated report"
        aggregate.group = JavaBasePlugin.VERIFICATION_GROUP

        aggregate.reports.configureConventions(project, "all")

        aggregate.onlyIf {
            aggregate.testTasks.size > 1
        }

        if (System.getProperty("idea.active") != null) {
            aggregate.extensions.extraProperties.set("idea.internal.test", true)
        }
    }

private fun cleanTaskName(taskName: String) = "clean" + taskName.capitalize()

@Suppress("UnstableApiUsage")
internal fun registerTestTask(taskHolder: TaskHolder<AbstractTestTask>) {
    val project = taskHolder.project
    val allTests = project.allTestsTask.doGetTask()

    val tasks = project.tasks
    tasks.maybeCreate(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(taskHolder.name)
    tasks.maybeCreate(cleanTaskName(allTests.name)).dependsOn(cleanTaskName(taskHolder.name))
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
                task.reports.html.isEnabled = false
                task.reports.junitXml.isEnabled = false

                allTests.checkFailedTests = true
                allTests.ignoreFailures = false
            }
        }

        ijListenTestTask(task)
    }
}
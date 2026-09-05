/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

/**
 * Adds a task dependency to all tasks named [task] from the given [projects].
 */
fun Task.dependsOnAll(task: String, projects: List<String>) {
    for (project in projects.distinct()) {
        dependsOn("$project:$task")
    }
}

/**
 * Registers a task that does not execute any tests by itself, but depends on other test tasks.
 * IntelliJ IDEA will recognize it as a test task and show the test execution UI.
 */
fun TaskContainer.testLifecycleTask(name: String, action: Action<TestLifecycleTask>): TaskProvider<TestLifecycleTask> {
    return register(name, TestLifecycleTask::class.java) {
        extensions.extraProperties["idea.internal.test"] = "true"
        action.execute(this)
    }
}

abstract class TestLifecycleTask : DefaultTask() {
    enum class QualityGate {
        /**
         * The given task is allowed to appear in any Quality Gate (or none).
         * This is useful when e.g. introducing a new task, allowing the TeamCity configuration
         * to pick up the change gradually. When a new task is introduced with the [QualityGate.Undefined],
         * and the TeamCity configuration 'caught up', then a second commit can promote the
         * given task to the correct [QualityGate]
         */
        Undefined,

        /**
         * This task is not expected to participate in any defined [QualityGate] (yet).
         */
        None,

        /**
         * The task is only checked during nightly builds.
         * The task is not executed before merging commits to master.
         */
        Nightly,

        /**
         * This task is required to be part of the 'master' quality gate.
         * Commits, merging to master, will have this task in the safe-merge aggregate.
         */
        Master
    }

    @get:Internal
    val qualityGate: Property<QualityGate> = project.objects.property(QualityGate::class.java)
        .convention(QualityGate.Undefined)

    init {
        group = "verification"
        description = "Lifecycle Test Task"
    }
}

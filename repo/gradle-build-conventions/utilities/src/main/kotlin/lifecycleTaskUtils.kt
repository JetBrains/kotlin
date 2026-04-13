/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer

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
fun TaskContainer.testLifecycleTask(name: String, action: Action<Task>) {
    register(name) {
        extensions.extraProperties["idea.internal.test"] = "true"
        action.execute(this)
    }
}

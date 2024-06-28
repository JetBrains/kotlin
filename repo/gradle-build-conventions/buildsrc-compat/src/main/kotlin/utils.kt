/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.RegisteringDomainObjectDelegateProviderWithAction
import org.gradle.kotlin.dsl.registering

fun Project.parallel(
    tasksToRun: List<TaskProvider<*>>,
    beforeAll: TaskProvider<*>? = null,
): RegisteringDomainObjectDelegateProviderWithAction<out TaskContainer, Task> {
    return tasks.registering {
        tasksToRun.forEach { dependsOn(it) }
    }.apply {
        if (beforeAll != null) {
            tasksToRun.forEach {
                it.configure {
                    dependsOn(beforeAll)
                }
            }
        }
    }
}


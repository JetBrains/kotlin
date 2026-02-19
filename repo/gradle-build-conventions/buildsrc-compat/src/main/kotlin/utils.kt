/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.newInstance

fun Project.parallel(
    tasksToRun: List<TaskProvider<*>>,
    beforeAll: TaskProvider<*>? = null,
): ConfigurableFileCollection {
    return objects.fileCollection().apply {
        from(tasksToRun)
        beforeAll?.let { initialTaskProvider ->
            tasksToRun.forEach { task ->
                task.configure {
                    inputs
                        .files(initialTaskProvider)
                        .withPathSensitivity(PathSensitivity.RELATIVE)
                        .withPropertyName("beforeAll")
                }
            }
        }
    }
}


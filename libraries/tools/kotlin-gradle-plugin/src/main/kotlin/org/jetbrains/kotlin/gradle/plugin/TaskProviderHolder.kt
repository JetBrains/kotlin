/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

class TaskProviderHolder<T : Task>(project: Project, private val name: String, type: Class<T>, configureAction: (T) -> (Unit)) :
    TaskHolder<T> {
    private val provider: TaskProvider<T> = project.tasks.register(name, type, configureAction)

    override fun getTaskOrProvider(): Any = provider

    override fun doGetTask(): T = provider.get()

    override fun configure(action: (T) -> (Unit)) {
        provider.configure(action)
    }

    override fun toString(): String {
        return "TaskProviderHolder instance: [className: ${javaClass.name}, task name: '$name']"
    }
}

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

import org.gradle.api.Task
import org.gradle.api.Project


/**
 * Reference to a org.gradle.api.Task or  org.gradle.api.TaskProvider necessary in order to support flexible creation of tasks.
 * For gradle versions < 4.9 tasks are created meanwhile for gradle with version >= 4.9 tasks are registered
 */
interface TaskHolder<T : Task> {

    /**
     * Returns Task itself if task was created or TaskProvider<Task> if task was registered.
     */
    fun getTaskOrProvider(): Any

    /**
     * Returns instance of task. If task created using lazy api, it will be instantiated
     */
    fun doGetTask(): T


    /**
     * Invokes task configuration. If task was registered the configuration action is added but not invoked
     */
    fun configure(action: (T) -> (Unit))
}

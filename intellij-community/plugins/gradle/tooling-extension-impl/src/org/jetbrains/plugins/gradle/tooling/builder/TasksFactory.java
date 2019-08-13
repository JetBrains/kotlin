/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.tooling.builder;

import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TasksFactory {
  private Map<Project, Set<Task>> allTasks;

  private void collectTasks(Project root) {
    allTasks = root.getAllTasks(true);
  }

  public Set<Task> getTasks(Project project) {
    if (allTasks == null) {
      collectTasks(project.getRootProject());
    }

    Set<Task> tasks = new LinkedHashSet<Task>(allTasks.get(project));
    for (Project subProject : project.getSubprojects()) {
      tasks.addAll(allTasks.get(subProject));
    }
    return tasks;
  }
}
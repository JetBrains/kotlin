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
import org.gradle.api.internal.tasks.DefaultTaskContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.util.GradleVersion;

import java.util.*;

public class TasksFactory {
  private final Map<Project, Set<Task>> allTasks = new HashMap<Project, Set<Task>>();
  private final Set<Project> processedRootProjects = new HashSet<Project>();

  private void collectTasks(Project root) {
    // Refresh tasks
    if (GradleVersion.current().compareTo(GradleVersion.version("5.0")) < 0) {
      TaskContainer tasks = root.getTasks();
      if (tasks instanceof DefaultTaskContainer) {
        ((DefaultTaskContainer)tasks).discoverTasks();
        SortedSet<String> taskNames = tasks.getNames();
        for (String taskName : taskNames) {
          tasks.findByName(taskName);
        }
      }
    }
    allTasks.putAll(root.getAllTasks(true));
  }

  public Set<Task> getTasks(Project project) {
    Project rootProject = project.getRootProject();
    if (processedRootProjects.add(rootProject)) {
      collectTasks(rootProject);
    }

    Set<Task> tasks = new LinkedHashSet<Task>(allTasks.get(project));
    for (Project subProject : project.getSubprojects()) {
      tasks.addAll(allTasks.get(subProject));
    }
    return tasks;
  }
}
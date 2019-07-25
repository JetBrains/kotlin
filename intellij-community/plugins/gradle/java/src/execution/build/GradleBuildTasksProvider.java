// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.build;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskPojo;
import com.intellij.task.ProjectModelBuildTask;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public interface GradleBuildTasksProvider {
  ExtensionPointName<GradleBuildTasksProvider> EP_NAME = ExtensionPointName.create("org.jetbrains.plugins.gradle.buildTasksProvider");

  boolean isApplicable(@NotNull ProjectModelBuildTask buildTask);

  void addBuildTasks(@NotNull ProjectModelBuildTask buildTask,
                     @NotNull Consumer<ExternalTaskPojo> cleanTasksConsumer,
                     @NotNull Consumer<ExternalTaskPojo> buildTasksConsumer);
}

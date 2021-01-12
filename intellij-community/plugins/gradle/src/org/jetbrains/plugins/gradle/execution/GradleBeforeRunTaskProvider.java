/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.execution;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import icons.GradleIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;

/**
 * @author Vladislav.Soroka
 */
public class GradleBeforeRunTaskProvider extends ExternalSystemBeforeRunTaskProvider {
  public static final Key<ExternalSystemBeforeRunTask> ID = Key.create("Gradle.BeforeRunTask");

  public GradleBeforeRunTaskProvider(Project project) {
    super(GradleConstants.SYSTEM_ID, project, ID);
  }

  @Override
  public Icon getIcon() {
    return GradleIcons.Gradle;
  }

  @Nullable
  @Override
  public Icon getTaskIcon(ExternalSystemBeforeRunTask task) {
    return GradleIcons.Gradle;
  }

  @Nullable
  @Override
  public ExternalSystemBeforeRunTask createTask(@NotNull RunConfiguration runConfiguration) {
    return new ExternalSystemBeforeRunTask(ID, GradleConstants.SYSTEM_ID);
  }
}

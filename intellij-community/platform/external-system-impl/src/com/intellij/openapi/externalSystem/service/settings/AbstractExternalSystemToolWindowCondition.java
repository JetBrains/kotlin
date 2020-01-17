/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.settings;

import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 */
public abstract class AbstractExternalSystemToolWindowCondition implements Condition<Project> {
  
  @NotNull private final ProjectSystemId myExternalSystemId;

  protected AbstractExternalSystemToolWindowCondition(@NotNull ProjectSystemId externalSystemId) {
    myExternalSystemId = externalSystemId;
  }

  @Override
  public boolean value(Project project) {
    ExternalSystemManager<?,?,?,?,?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
    if (manager == null) {
      return false;
    }
    AbstractExternalSystemSettings<?, ?,?> settings = manager.getSettingsProvider().fun(project);
    return settings != null && !settings.getLinkedProjectsSettings().isEmpty();
  }
}

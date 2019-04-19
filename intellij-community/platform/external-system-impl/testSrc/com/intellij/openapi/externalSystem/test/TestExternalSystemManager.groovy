/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.test

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.openapi.externalSystem.ExternalSystemManager
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Pair
import com.intellij.util.Function
import org.jetbrains.annotations.NotNull
/**
 * @author Denis Zhdanov
 */
class TestExternalSystemManager implements ExternalSystemManager<
TestExternalProjectSettings,
TestExternalSystemSettingsListener,
TestExternalSystemSettings,
TestExternalSystemLocalSettings,
TestExternalSystemExecutionSettings>
{

  TestExternalSystemSettings systemSettings
  TestExternalSystemLocalSettings localSettings
  TestExternalSystemExecutionSettings executionSettings

  TestExternalSystemManager(@NotNull Project project) {
    Disposer.register(project, systemSettings = new TestExternalSystemSettings(project))
    localSettings = new TestExternalSystemLocalSettings(project)
    executionSettings = new TestExternalSystemExecutionSettings()
  }

  @NotNull
  @Override
  ProjectSystemId getSystemId() {
    ExternalSystemTestUtil.TEST_EXTERNAL_SYSTEM_ID
  }

  @NotNull
  @Override
  Function<Project, TestExternalSystemSettings> getSettingsProvider() { { project -> systemSettings } as Function }

  @NotNull
  @Override
  Function<Project, TestExternalSystemLocalSettings> getLocalSettingsProvider() { { project -> localSettings } as Function }

  @NotNull
  @Override
  Function<Pair<Project, String>, TestExternalSystemExecutionSettings> getExecutionSettingsProvider() {
    { project -> executionSettings } as Function
  }

  @NotNull
  @Override
  Class<? extends ExternalSystemProjectResolver<TestExternalSystemExecutionSettings>> getProjectResolverClass() {
    throw new UnsupportedOperationException()
  }

  @Override
  Class<? extends ExternalSystemTaskManager<TestExternalSystemExecutionSettings>> getTaskManagerClass() {
    throw new UnsupportedOperationException()
  }

  @NotNull
  @Override
  FileChooserDescriptor getExternalProjectDescriptor() {
    throw new UnsupportedOperationException()
  }

  @Override
  void enhanceRemoteProcessing(@NotNull SimpleJavaParameters parameters) throws ExecutionException {
  }
}

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
package org.jetbrains.plugins.gradle.execution.filters;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.openapi.externalSystem.importing.ImportSpec;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemResolveProjectTask;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class ReRunSyncFilter extends GradleReRunBuildFilter {
  private final ExternalSystemResolveProjectTask myTask;
  private final Project myProject;

  public ReRunSyncFilter(ExternalSystemResolveProjectTask task, Project project) {
    super(task.getExternalProjectPath());
    myTask = task;
    myProject = project;
  }

  @NotNull
  @Override
  protected HyperlinkInfo getHyperLinkInfo(List<String> options) {
    return (project) -> {
      ImportSpec importSpec = new ImportSpecBuilder(myProject, myTask.getExternalSystemId())
        .withArguments(StringUtil.join(options, " "))
        .build();
      ExternalSystemUtil.refreshProject(myTask.getExternalProjectPath(), importSpec);
    };
  }
}

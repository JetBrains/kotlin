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

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemExecuteTaskTask;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class ReRunTaskFilter extends GradleReRunBuildFilter {
  private final ExecutionEnvironment myEnv;

  public ReRunTaskFilter(ExternalSystemExecuteTaskTask task, ExecutionEnvironment env) {
    super(task.getExternalProjectPath());
    myEnv = env;
  }

  @NotNull
  @Override
  protected HyperlinkInfo getHyperLinkInfo(List<String> options) {
    return (project) -> {
      RunnerAndConfigurationSettings settings = myEnv.getRunnerAndConfigurationSettings();
      if (settings == null) return;
      RunConfiguration conf = settings.getConfiguration();
      if (!(conf instanceof ExternalSystemRunConfiguration)) return;

      ExternalSystemTaskExecutionSettings taskExecutionSettings = ((ExternalSystemRunConfiguration)conf).getSettings();
      String scriptParameters = taskExecutionSettings.getScriptParameters();
      List<String> params;
      if (StringUtil.isEmpty(scriptParameters)) {
        params = new SmartList<>();
      }
      else {
        params = StringUtil.split(scriptParameters, " ");
        params.remove("--stacktrace");
        params.remove("--info");
        params.remove("--debug");
      }
      params.addAll(options);
      taskExecutionSettings.setScriptParameters(StringUtil.join(params, " "));

      ExecutionUtil.restart(myEnv);
    };
  }
}

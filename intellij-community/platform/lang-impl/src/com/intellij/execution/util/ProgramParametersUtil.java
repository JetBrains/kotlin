/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.execution.util;

import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.execution.configurations.SimpleProgramParameters;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public class ProgramParametersUtil {
  public static void configureConfiguration(SimpleProgramParameters parameters, CommonProgramRunConfigurationParameters configuration) {
    new ProgramParametersConfigurator().configureConfiguration(parameters, configuration);
  }

  public static String getWorkingDir(CommonProgramRunConfigurationParameters configuration, Project project, Module module) {
    return new ProgramParametersConfigurator().getWorkingDir(configuration, project, module);
  }

  public static void checkWorkingDirectoryExist(CommonProgramRunConfigurationParameters configuration, Project project, Module module)
    throws RuntimeConfigurationWarning {
    new ProgramParametersConfigurator().checkWorkingDirectoryExist(configuration, project, module);
  }

  public static String expandPath(String path, Module module, Project project) {
    return new ProgramParametersConfigurator().expandPath(path, module, project);
  }

  @Nullable
  public static Module getModule(CommonProgramRunConfigurationParameters configuration) {
    return new ProgramParametersConfigurator().getModule(configuration);
  }
}

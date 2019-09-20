// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleConsoleProperties;

/**
 * @author Vladislav.Soroka
 */
public class GradleJavaIdeManager extends GradleIdeManager {

  @Override
  public SMTRunnerConsoleProperties createTestConsoleProperties(Project project, Executor executor, RunConfiguration runConfiguration) {
    if (runConfiguration instanceof ExternalSystemRunConfiguration) {
      return new GradleConsoleProperties((ExternalSystemRunConfiguration)runConfiguration, executor);
    }
    return null;
  }
}

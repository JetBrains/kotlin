// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.startup;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.GradleBuildClasspathManager;

/**
 * @author Vladislav.Soroka
 */
public class GradleJvmStartupActivity implements StartupActivity {
  @Override
  public void runActivity(@NotNull final Project project) {
    configureBuildClasspath(project);
  }

  private static void configureBuildClasspath(@NotNull final Project project) {
    GradleBuildClasspathManager.getInstance(project).reload();
  }
}

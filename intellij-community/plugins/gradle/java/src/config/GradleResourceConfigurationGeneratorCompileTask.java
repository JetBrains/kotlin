// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class GradleResourceConfigurationGeneratorCompileTask implements CompileTask {

  @Override
  public boolean execute(@NotNull CompileContext context) {
    Project project = context.getProject();
    GradleResourceCompilerConfigurationGenerator buildConfigurationGenerator =
      ServiceManager.getService(project, GradleResourceCompilerConfigurationGenerator.class);
    ApplicationManager.getApplication().runReadAction(() -> buildConfigurationGenerator.generateBuildConfiguration(context));
    return true;
  }
}

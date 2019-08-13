// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.server.impl;

import com.intellij.compiler.server.BuildProcessParametersProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Adds custom plugins configured in {@link BuildProcessCustomPluginsConfiguration} to the build process' classpath.
 */
public class CustomBuildProcessPluginsClasspathProvider extends BuildProcessParametersProvider {
  private final Project myProject;

  public CustomBuildProcessPluginsClasspathProvider(Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public List<String> getClassPath() {
    return BuildProcessCustomPluginsConfiguration.getInstance(myProject).getCustomPluginsClasspath();
  }
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * @author yole
 */
public interface CommandLineInspectionProjectConfigurator {
  ExtensionPointName<CommandLineInspectionProjectConfigurator> EP_NAME = ExtensionPointName.create("com.intellij.commandLineInspectionProjectConfigurator");

  boolean isApplicable(Path projectPath);

  /**
   * Invoked before a project is imported.
   */
  void configureEnvironment();

  void configureProject(@NotNull Project project);
}

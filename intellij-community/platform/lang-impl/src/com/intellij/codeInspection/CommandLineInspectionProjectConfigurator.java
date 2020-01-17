// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * @author yole
 */
public interface CommandLineInspectionProjectConfigurator {
  ExtensionPointName<CommandLineInspectionProjectConfigurator> EP_NAME = ExtensionPointName.create("com.intellij.commandLineInspectionProjectConfigurator");

  /**
   * Returns true if any additional configuration is required to inspect the project at the given path.
   */
  boolean isApplicable(@NotNull Path projectPath, @NotNull CommandLineInspectionProgressReporter logger);

  /**
   * Invoked before a project is imported.
   */
  default void configureEnvironment(@NotNull Path projectPath, @NotNull CommandLineInspectionProgressReporter logger) {
  }

  /**
   * Invoked after the project has been imported and before the analysis on the specified scope
   * is started.
   */
  default void configureProject(@NotNull Project project, @NotNull AnalysisScope scope, @NotNull CommandLineInspectionProgressReporter logger) {
  }
}

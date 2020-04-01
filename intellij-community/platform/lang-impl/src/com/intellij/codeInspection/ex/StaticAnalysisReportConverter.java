// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ex;

import com.intellij.codeInspection.ProjectDescriptionUtilKt;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;

public class StaticAnalysisReportConverter extends JsonInspectionsReportConverter {
  @Override
  public String getFormatName() {
    return "sa";
  }

  @Override
  public void projectData(@NotNull Project project, @Nullable String outputPath) {
    if (outputPath == null) return;
    ProjectDescriptionUtilKt.writeProjectDescription(Paths.get(outputPath).resolve("projectStructure.json"), project);
  }
}

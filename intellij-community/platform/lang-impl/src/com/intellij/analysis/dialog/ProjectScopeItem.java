/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.analysis.dialog;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.project.Project;

public class ProjectScopeItem implements ModelScopeItem {
  public final Project project;

  public ProjectScopeItem(Project project) {
    this.project = project;
  }

  @Override
  public AnalysisScope getScope() {
    return new AnalysisScope(project);
  }
}
/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.packageDependencies.actions;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.project.Project;
import com.intellij.packageDependencies.BackwardDependenciesBuilder;
import com.intellij.packageDependencies.DependenciesBuilder;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BackwardDependenciesHandler extends DependenciesHandlerBase {
  private final AnalysisScope myScopeOfInterest;

  public BackwardDependenciesHandler(@NotNull Project project, AnalysisScope scope, final AnalysisScope selectedScope) {
    this(project, Collections.singletonList(scope), selectedScope, new HashSet<>());
  }

  public BackwardDependenciesHandler(@NotNull Project project, final List<? extends AnalysisScope> scopes, @Nullable final AnalysisScope scopeOfInterest, Set<PsiFile> excluded) {
    super(project, scopes, excluded);
    myScopeOfInterest = scopeOfInterest;
  }

  @Override
  protected String getProgressTitle() {
    return CodeInsightBundle.message("backward.dependencies.progress.text");
  }

  @Override
  protected String getPanelDisplayName(final AnalysisScope scope) {
    return CodeInsightBundle.message("backward.dependencies.toolwindow.title", scope.getDisplayName());
  }

  @Override
  protected DependenciesBuilder createDependenciesBuilder(AnalysisScope scope) {
    return new BackwardDependenciesBuilder(myProject, scope, myScopeOfInterest);
  }
}

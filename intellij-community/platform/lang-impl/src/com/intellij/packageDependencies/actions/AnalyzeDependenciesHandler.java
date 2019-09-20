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
import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.openapi.project.Project;
import com.intellij.packageDependencies.DependenciesBuilder;
import com.intellij.packageDependencies.ForwardDependenciesBuilder;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnalyzeDependenciesHandler extends DependenciesHandlerBase {
  private final int myTransitiveBorder;

  public AnalyzeDependenciesHandler(@NotNull Project project, List<? extends AnalysisScope> scopes, int transitiveBorder, Set<PsiFile> excluded) {
    super(project, scopes, excluded);
    myTransitiveBorder = transitiveBorder;
  }

  public AnalyzeDependenciesHandler(final Project project, final AnalysisScope scope, final int transitiveBorder) {
    this(project, Collections.singletonList(scope), transitiveBorder, new HashSet<>());
  }

  @Override
  protected DependenciesBuilder createDependenciesBuilder(AnalysisScope scope) {
    return new ForwardDependenciesBuilder(myProject, scope, myTransitiveBorder);
  }

  @Override
  protected String getPanelDisplayName(final AnalysisScope scope) {
    return AnalysisScopeBundle.message("package.dependencies.toolwindow.title", scope.getDisplayName());
  }

  @Override
  protected String getProgressTitle() {
    return AnalysisScopeBundle.message("package.dependencies.progress.title");
  }
}
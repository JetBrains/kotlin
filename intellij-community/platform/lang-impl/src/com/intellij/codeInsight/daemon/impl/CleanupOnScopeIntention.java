/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.daemon.impl;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.analysis.AnalysisUIOptions;
import com.intellij.analysis.BaseAnalysisActionDialog;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.actions.CleanupIntention;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

public class CleanupOnScopeIntention extends CleanupIntention {
  public static final CleanupOnScopeIntention INSTANCE = new CleanupOnScopeIntention();

  private CleanupOnScopeIntention() {}

  @Nullable
  @Override
  protected AnalysisScope getScope(final Project project, final PsiFile file) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(file);
    AnalysisScope analysisScope = new AnalysisScope(file);
    final VirtualFile virtualFile = file.getVirtualFile();
    if (file.isPhysical() || virtualFile == null || !virtualFile.isInLocalFileSystem()) {
      analysisScope = new AnalysisScope(project);
    }
    final BaseAnalysisActionDialog dlg = new BaseAnalysisActionDialog(
      AnalysisScopeBundle.message("specify.analysis.scope", InspectionsBundle.message("inspection.action.title")),
      AnalysisScopeBundle.message("analysis.scope.title", InspectionsBundle.message("inspection.action.noun")), project, BaseAnalysisActionDialog.standardItems(
      project, analysisScope, module, file),
      AnalysisUIOptions.getInstance(project), true);
    if (!dlg.showAndGet()) {
      return null;
    }
    final AnalysisUIOptions uiOptions = AnalysisUIOptions.getInstance(project);
    return dlg.getScope(uiOptions, analysisScope, project, module);
  }
}

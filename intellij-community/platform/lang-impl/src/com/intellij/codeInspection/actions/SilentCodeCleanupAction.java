// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.actions;

import com.intellij.analysis.AnalysisActionUtils;
import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.GlobalInspectionContextBase;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SilentCodeCleanupAction extends AnAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    e.getPresentation().setEnabled(project != null && !DumbService.isDumb(project) && getInspectionScope(e.getDataContext(), project) != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    AnalysisScope analysisScope = getInspectionScope(e.getDataContext(), project);
    if (analysisScope == null)
      return;

    FileDocumentManager.getInstance().saveAllDocuments();

    FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassist.inspect.batch");
    runInspections(project, analysisScope);
  }

  @SuppressWarnings("WeakerAccess")
  @Nullable
  protected Runnable getPostRunnable() { return null; }

  @SuppressWarnings("WeakerAccess")
  protected void runInspections(@NotNull Project project, @NotNull AnalysisScope scope) {
    InspectionProfile profile = getProfileForSilentCleanup(project);
    if (profile == null) {
      return;
    }
    InspectionManager managerEx = InspectionManager.getInstance(project);
    GlobalInspectionContextBase globalContext = (GlobalInspectionContextBase) managerEx.createNewGlobalContext();
    globalContext.codeCleanup(scope, profile, getTemplatePresentation().getText(), getPostRunnable(), false);
  }

  @SuppressWarnings("WeakerAccess")
  @Nullable
  protected InspectionProfile getProfileForSilentCleanup(@NotNull Project project) {
    return InspectionProjectProfileManager.getInstance(project).getCurrentProfile();
  }

  @Nullable
  @SuppressWarnings("WeakerAccess")
  protected AnalysisScope getInspectionScope(@NotNull DataContext dataContext, @NotNull Project project) {
    return AnalysisActionUtils.getInspectionScope(dataContext, project, false);
  }
}

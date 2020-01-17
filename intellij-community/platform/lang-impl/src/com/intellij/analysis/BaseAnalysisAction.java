// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis;

import com.intellij.analysis.dialog.ModelScopeItem;
import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public abstract class BaseAnalysisAction extends AnAction {
  private static final String DIMENSION_KEY_PREFIX = "ANALYSIS_DLG_";

  private final String myTitle;
  private final String myAnalysisNoon;

  protected BaseAnalysisAction(@Nls(capitalization = Nls.Capitalization.Title) String title, String analysisNoon) {
    myTitle = title;
    myAnalysisNoon = analysisNoon;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    e.getPresentation().setEnabled(project != null && !DumbService.isDumb(project) && getInspectionScope(e.getDataContext(), project) != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;
    DataContext dataContext = e.getDataContext();
    AnalysisScope scope = getInspectionScope(dataContext, project);
    if (scope == null) return;

    String title = getDialogTitle();
    String noon = AnalysisScopeBundle.message("analysis.scope.title", myAnalysisNoon);
    Module module = getModuleFromContext(dataContext);
    boolean rememberScope = ActionPlaces.isMainMenuOrActionSearch(e.getPlace());
    AnalysisUIOptions uiOptions = AnalysisUIOptions.getInstance(project);
    PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    List<ModelScopeItem> items = BaseAnalysisActionDialog.standardItems(project, scope, module, element);
    BaseAnalysisActionDialog dlg = new BaseAnalysisActionDialog(title, noon, project, items, uiOptions, rememberScope) {
      @Override
      protected String getDimensionServiceKey() {
        return DIMENSION_KEY_PREFIX + getClass().getName();
      }

      @Override
      protected JComponent getAdditionalActionSettings(Project project) {
        return BaseAnalysisAction.this.getAdditionalActionSettings(project, this);
      }

      @Override
      protected String getHelpId() {
        return getHelpTopic();
      }
    };
    if (!dlg.showAndGet()) {
      canceled();
      return;
    }

    int oldScopeType = uiOptions.SCOPE_TYPE;
    scope = dlg.getScope(scope);
    if (!rememberScope) {
      uiOptions.SCOPE_TYPE = oldScopeType;
    }
    uiOptions.ANALYZE_TEST_SOURCES = dlg.isInspectTestSources();

    FileDocumentManager.getInstance().saveAllDocuments();
    analyze(project, scope);
  }

  protected @NotNull String getDialogTitle() {
    return AnalysisScopeBundle.message("specify.analysis.scope", myTitle);
  }

  protected String getHelpTopic() {
    return "reference.dialogs.analyzeDependencies.scope";
  }

  protected void canceled() { }

  protected abstract void analyze(@NotNull Project project, @NotNull AnalysisScope scope);

  @Nullable
  private AnalysisScope getInspectionScope(@NotNull DataContext dataContext, @NotNull Project project) {
    return AnalysisActionUtils.getInspectionScope(dataContext, project, acceptNonProjectDirectories());
  }

  protected boolean acceptNonProjectDirectories() {
    return false;
  }

  @Nullable
  protected JComponent getAdditionalActionSettings(Project project, BaseAnalysisActionDialog dialog) {
    return null;
  }

  @Nullable
  private static Module getModuleFromContext(@NotNull DataContext dataContext) {
    InspectionResultsView inspectionView = dataContext.getData(InspectionResultsView.DATA_KEY);
    if (inspectionView != null) {
      AnalysisScope scope = inspectionView.getScope();
      if (scope.getScopeType() == AnalysisScope.MODULE && scope.isValid()) {
        return scope.getModule();
      }
    }
    return dataContext.getData(LangDataKeys.MODULE);
  }
}
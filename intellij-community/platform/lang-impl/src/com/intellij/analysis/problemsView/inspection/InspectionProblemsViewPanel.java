// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.inspection;

import com.intellij.analysis.problemsView.AnalysisProblem;
import com.intellij.analysis.problemsView.AnalysisProblemBundle;
import com.intellij.analysis.problemsView.AnalysisProblemsTableModel;
import com.intellij.analysis.problemsView.AnalysisProblemsViewPanel;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.OpenSourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

class InspectionProblemsViewPanel extends AnalysisProblemsViewPanel {
  @NotNull private final InspectionProblemsPresentationHelper myPresentationHelper;

  InspectionProblemsViewPanel(@NotNull Project project, @NotNull InspectionProblemsPresentationHelper presentationHelper) {
    super(project, presentationHelper);
    myPresentationHelper = presentationHelper;
  }

  @Override
  protected void addQuickFixActions(@NotNull final DefaultActionGroup group, @Nullable final AnalysisProblem problem) {
    final VirtualFile selectedVFile = problem != null ? problem.getFile() : null;
    if (selectedVFile == null) return;
    HighlightingProblem hp = (HighlightingProblem)problem;
    List<Pair<HighlightInfo.IntentionActionDescriptor, RangeMarker>> fixes = hp.getFixes();
    boolean found = false;
    for (Pair<HighlightInfo.IntentionActionDescriptor, RangeMarker> pair : fixes) {
      IntentionAction fix = pair.getFirst().getAction();
      RangeMarker fixMarker = pair.getSecond();
      PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(fixMarker.getDocument());
      PsiFile top = file == null ? null : InjectedLanguageManager.getInstance(myProject).getTopLevelFile(file);
      if (top != null) {
        if (!found) {
          found = true;
          group.addSeparator();
        }
        group.add(new AnAction(fix.getText(), fix.getText(), AllIcons.Actions.QuickfixBulb) {
          @Override
          public void actionPerformed(@NotNull final AnActionEvent event) {
            OpenSourceUtil.navigate(PsiNavigationSupport.getInstance().createNavigatable(myProject, selectedVFile, fixMarker.getStartOffset()));
            Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
            ShowIntentionActionsHandler.chooseActionAndInvoke(top, editor, fix, fix.getText());
          }

          @Override
          public void update(@NotNull AnActionEvent e) {
            boolean valid = fixMarker.isValid() && top.isValid();
            e.getPresentation().setEnabledAndVisible(valid);
          }
        });
      }
    }
  }

  @Override
  protected void updateStatusDescription() {
    InspectionProblemsView problemsView = InspectionProblemsView.getInstance(myProject);
    problemsView.setHeaderText(getModel().getStatusText());
    problemsView.setToolWindowIcon(getStatusIcon());
  }

  @Override
  protected void addActionsTo(@NotNull DefaultActionGroup group) {
    //addReanalyzeActions(group);
    //group.addAction(new InspectionProblemsViewPanel.AnalysisServerSettingsAction());
    group.addSeparator();

    addAutoScrollToSourceAction(group);
    addGroupBySeverityAction(group);
    group.addAction(new FilterProblemsAction());
    group.addSeparator();
  }

  private class FilterProblemsAction extends DumbAwareAction implements Toggleable {
    FilterProblemsAction() {
      super(AnalysisProblemBundle.messagePointer("filter.problems"), AnalysisProblemBundle.messagePointer("filter.problems.description"), AllIcons.General.Filter);
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      // show icon as toggled on if any filter is active
      Toggleable.setSelected(e.getPresentation(), myPresentationHelper.areFiltersApplied());
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
      showFiltersPopup();
    }
  }

  private void showFiltersPopup() {
    final InspectionProblemsFilterForm filterForm = new InspectionProblemsFilterForm();
    filterForm.reset(getPresentationHelper());
    filterForm.addListener(new InspectionProblemsFilterForm.FilterListener() {
      @Override
      public void filtersChanged() {
        getPresentationHelper().updateFromFilterSettingsUI(filterForm);
        fireGroupingOrFilterChanged();
      }

      @Override
      public void filtersResetRequested() {
        getPresentationHelper().resetAllFilters();
        filterForm.reset(getPresentationHelper());
        fireGroupingOrFilterChanged();
      }
    });

    createAndShowPopup("Problems Filter", filterForm.getMainPanel());
  }

  private @NotNull InspectionProblemsPresentationHelper getPresentationHelper() {
    return myPresentationHelper;
  }

  private void addProblem(@NotNull AnalysisProblem problem) {
    AnalysisProblem oldSelectedProblem = myTable.getSelectedObject();
    AnalysisProblemsTableModel model = getModel();
    model.removeRows(p -> p.equals(problem));
    AnalysisProblem updatedSelectedProblem = model.addProblemsAndReturnReplacementForSelection(Collections.singletonList(problem), oldSelectedProblem);

    if (updatedSelectedProblem != null) {
      myTable.setSelection(Collections.singletonList(updatedSelectedProblem));
    }

    updateStatusDescription();
  }

  private Disposable myCurrentFileDisposable = Disposer.newDisposable();
  public void setCurrentFile(@Nullable VirtualFile file) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    AnalysisProblemsTableModel model = getModel();
    model.removeRows(problem -> true); // remove everything, in case setCurrentFile() was called with the same file to refresh from highlights
    if (file != null) {
      PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
      Document document = psiFile == null ? null : PsiDocumentManager.getInstance(myProject).getDocument(psiFile);
      if (document != null) {
        Disposer.dispose(myCurrentFileDisposable);
        myCurrentFileDisposable = Disposer.newDisposable();
        Disposer.register(myProject, myCurrentFileDisposable);
        MarkupModelEx markupModelEx = (MarkupModelEx)DocumentMarkupModel.forDocument(document, myProject, true);
        markupModelEx.addMarkupModelListener(myCurrentFileDisposable,
           new MarkupModelListener() {
              @Override
              public void afterAdded(@NotNull RangeHighlighterEx highlighter) {
                highlighterAdded(highlighter);
              }

              @Override
              public void beforeRemoved(@NotNull RangeHighlighterEx highlighter) {
                highlighterRemoved(highlighter);
              }
            });
        markupModelEx.processRangeHighlightersOverlappingWith(0, document.getTextLength(), r->{
          highlighterAdded(r);
          return true;
        });
      }
    }
    updateStatusDescription();
  }

  private void highlighterRemoved(@NotNull RangeHighlighterEx highlighter) {
    Object tooltip = highlighter.getErrorStripeTooltip();
    if (tooltip instanceof HighlightInfo) {
      HighlightInfo info = (HighlightInfo)tooltip;
      if (info.getDescription() != null) {
        HighlightingProblem problem = new HighlightingProblem(myProject, myPresentationHelper.getCurrentFile(), info);
        getModel().removeRows(p -> p.equals(problem));
        updateStatusDescription();
      }
    }
  }

  private void highlighterAdded(@NotNull RangeHighlighterEx highlighter) {
    Object tooltip = highlighter.getErrorStripeTooltip();
    if (tooltip instanceof HighlightInfo) {
      HighlightInfo info = (HighlightInfo)tooltip;
      if (info.getDescription() != null) {
        HighlightingProblem problem = new HighlightingProblem(myProject, myPresentationHelper.getCurrentFile(), info);
        addProblem(problem);
      }
    }
  }

  @Override
  protected @NotNull Icon getStatusIcon() {
    final AnalysisProblemsTableModel model = getModel();
    return model.hasErrors() ? AllIcons.Toolwindows.ErrorEvents : model.hasWarnings() ? AllIcons.Toolwindows.Problems : AllIcons.Toolwindows.NoEvents;
  }

  //private void showFiltersPopup() {
  //  InspectionProblemsFilterForm filterForm = new DartProblemsFilterForm();
  //  filterForm.reset(myPresentationHelper);
  //  filterForm.addListener(new DartProblemsFilterForm.FilterListener() {
  //    @Override
  //    public void filtersChanged() {
  //      myPresentationHelper.updateFromFilterSettingsUI(filterForm);
  //      fireGroupingOrFilterChanged();
  //    }
  //
  //    @Override
  //    public void filtersResetRequested() {
  //      myPresentationHelper.resetAllFilters();
  //      filterForm.reset(myPresentationHelper);
  //      fireGroupingOrFilterChanged();
  //    }
  //  });
  //
  //  createAndShowPopup("Dart Problems Filter", filterForm.getMainPanel());
  //}
  //
  //private void showAnalysisServerSettingsPopup() {
  //  final DartAnalysisServerSettingsForm serverSettingsForm = new DartAnalysisServerSettingsForm(myProject);
  //  serverSettingsForm.reset(myPresentationHelper);
  //
  //  serverSettingsForm.addListener(() -> {
  //    myPresentationHelper.updateFromServerSettingsUI(serverSettingsForm);
  //    DartAnalysisServerService.getInstance(myProject).ensureAnalysisRootsUpToDate();
  //  });
  //
  //  createAndShowPopup(DartBundle.message("analysis.server.settings.title"), serverSettingsForm.getMainPanel());
  //}
  //
  //private class FilterProblemsAction extends DumbAwareAction implements Toggleable {
  //  FilterProblemsAction() {
  //    super(DartBundle.messagePointer("filter.problems"), DartBundle.messagePointer("filter.problems.description"), AllIcons.General.Filter);
  //  }
  //
  //  @Override
  //  public void update(@NotNull final AnActionEvent e) {
  //    // show icon as toggled on if any filter is active
  //    Toggleable.setSelected(e.getPresentation(), myPresentationHelper.areFiltersApplied());
  //  }
  //
  //  @Override
  //  public void actionPerformed(@NotNull final AnActionEvent e) {
  //    showFiltersPopup();
  //  }
  //}

}

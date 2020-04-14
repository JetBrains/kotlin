// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.actions;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction;
import com.intellij.find.FindBundle;
import com.intellij.find.FindManager;
import com.intellij.find.FindSettings;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.find.usages.SearchTarget;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorActivityManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.SearchScope;
import com.intellij.usages.PsiElementUsageTarget;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.UsageView;
import org.jetbrains.annotations.NotNull;

import static com.intellij.find.actions.FindUsagesKt.findUsages;
import static com.intellij.find.actions.ResolverKt.findShowUsages;

public class FindUsagesAction extends AnAction {

  public FindUsagesAction() {
    setInjectedContext(true);
  }

  protected boolean toShowDialog() {
    return false;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    DataContext dataContext = e.getDataContext();
    if (Registry.is("ide.symbol.find.usages")) {
      findSymbolUsages(project, dataContext);
    }
    else {
      findUsageTargetUsages(project, dataContext);
    }
  }

  private void findSymbolUsages(@NotNull Project project, @NotNull DataContext dataContext) {
    findShowUsages(project, dataContext, FindBundle.message("find.usages.ambiguous.title"), new UsageVariantHandler() {

      @Override
      public void handleTarget(@NotNull SearchTarget target) {
        SearchScope searchScope = FindUsagesOptions.findScopeByName(project, dataContext, FindSettings.getInstance().getDefaultScopeName());
        findUsages(toShowDialog(), project, searchScope, target);
      }

      @Override
      public void handlePsi(@NotNull PsiElement element) {
        startFindUsages(element);
      }
    });
  }

  private void findUsageTargetUsages(@NotNull Project project, @NotNull DataContext dataContext) {
    UsageTarget[] usageTargets = dataContext.getData(UsageView.USAGE_TARGETS_KEY);
    if (usageTargets == null) {
      final Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
      chooseAmbiguousTargetAndPerform(project, editor, element -> {
        startFindUsages(element);
        return false;
      });
    }
    else {
      UsageTarget target = usageTargets[0];
      if (target instanceof PsiElementUsageTarget) {
        PsiElement element = ((PsiElementUsageTarget)target).getElement();
        if (element != null) {
          startFindUsages(element);
        }
      }
      else {
        target.findUsages();
      }
    }
  }

  protected void startFindUsages(@NotNull PsiElement element) {
    FindManager.getInstance(element.getProject()).findUsages(element);
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    FindUsagesInFileAction.updateFindUsagesAction(event);
  }

  static void chooseAmbiguousTargetAndPerform(@NotNull final Project project,
                                              final Editor editor,
                                              @NotNull PsiElementProcessor<? super PsiElement> processor) {
    if (editor == null) {
      Messages.showMessageDialog(project, FindBundle.message("find.no.usages.at.cursor.error"), CommonBundle.getErrorTitle(),
                                 Messages.getErrorIcon());
    }
    else {
      int offset = editor.getCaretModel().getOffset();
      boolean chosen = GotoDeclarationAction.chooseAmbiguousTarget(
        project, editor, offset, processor, FindBundle.message("find.usages.ambiguous.title"), null
      );
      if (!chosen) {
        ApplicationManager.getApplication().invokeLater(() -> {
          if (editor.isDisposed() || !EditorActivityManager.getInstance().isVisible(editor)) return;
          HintManager.getInstance().showErrorHint(editor, FindBundle.message("find.no.usages.at.cursor.error"));
        }, project.getDisposed());
      }
    }
  }

  public static class ShowSettingsAndFindUsages extends FindUsagesAction {
    @Override
    protected void startFindUsages(@NotNull PsiElement element) {
      FindManager.getInstance(element.getProject()).findUsages(element, true);
    }

    @Override
    protected boolean toShowDialog() {
      return true;
    }
  }
}

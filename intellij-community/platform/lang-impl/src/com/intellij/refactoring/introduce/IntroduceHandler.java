// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.introduce;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.IntroduceTargetChooser;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.introduce.inplace.AbstractInplaceIntroducer;
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for Introduce variable/field/etc refactorings. It provides skeleton for choosing the target and consequent invoking of the
 * given in-place introducer.
 */
public abstract class IntroduceHandler<Target extends IntroduceTarget, Scope extends PsiElement> implements RefactoringActionHandler {

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    if (editor == null || file == null) {
      return;
    }

    if (!CommonRefactoringUtil.checkReadOnlyStatus(project, file)) {
      return;
    }

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    int caretCount = editor.getCaretModel().getCaretCount();
    if (caretCount != 1) {
      cannotPerformRefactoring(project, editor);
      return;
    }

    SelectionModel selectionModel = editor.getSelectionModel();

    if (selectionModel.hasSelection()) {
      invokeOnSelection(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), project, editor, file);
    }
    else {
      Pair<List<Target>, Integer> targetInfo = collectTargets(file, editor, project);
      List<Target> list = targetInfo.getFirst();
      if (list.isEmpty()) {
        cannotPerformRefactoring(project, editor);
      }
      else if (list.size() == 1) {
        invokeOnTarget(list.get(0), file, editor, project);
      }
      else {
        IntroduceTargetChooser.showIntroduceTargetChooser(editor, list, new Pass<Target>() {
          @Override
          public void pass(final Target target) {
            invokeOnTarget(target, file, editor, project);
          }
        }, "Expressions", targetInfo.getSecond());
      }
    }
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
    //not supported
  }

  private void invokeOnTarget(@NotNull Target target,
                              @NotNull PsiFile file,
                              @NotNull Editor editor,
                              @NotNull Project project) {
    String message = checkSelectedTarget(target, file, editor, project);
    if (message != null) {
      showErrorHint(message, editor, project);
      return;
    }

    invokeScopeStep(target, file, editor, project);
  }

  private void invokeOnSelection(int start,
                                 int end,
                                 @NotNull Project project,
                                 @NotNull Editor editor,
                                 @NotNull PsiFile file) {
    Target target = findSelectionTarget(start, end, file, editor, project);
    if (target != null) {
      invokeScopeStep(target, file, editor, project);
    }
    else {
      cannotPerformRefactoring(project, editor);
    }
  }

  private void invokeScopeStep(@NotNull Target target,
                               @NotNull PsiFile file,
                               @NotNull Editor editor,
                               @NotNull Project project) {
    List<Scope> scopes = collectTargetScopes(target, editor, file, project);

    if (scopes.isEmpty()) {
      String message = RefactoringBundle.getCannotRefactorMessage(getEmptyScopeErrorMessage());
      showErrorHint(message, editor, project);
      return;
    }

    if (scopes.size() == 1) {
      invokeFindUsageStep(target, scopes.get(0), file, editor, project);
    }
    else {
      @SuppressWarnings("unchecked") Scope[] scopeArray = (Scope[])PsiUtilCore.toPsiElementArray(scopes);
      NavigationUtil.getPsiElementPopup(scopeArray, getScopeRenderer(), getChooseScopeTitle(), scope -> {
        invokeFindUsageStep(target, scope, file, editor, project);
        return false;
      }).showInBestPositionFor(editor);
    }
  }

  private void invokeFindUsageStep(@NotNull Target target,
                                   @NotNull Scope scope,
                                   @NotNull PsiFile file,
                                   @NotNull Editor editor,
                                   @NotNull Project project) {
    List<UsageInfo> usages = collectUsages(target, scope);
    String message = checkUsages(usages);
    if (message != null) {
      showErrorHint(message, editor, project);
      return;
    }
    invokeDialogStep(target, scope, usages, file, editor, project);
  }

  private void invokeDialogStep(@NotNull Target target,
                                @NotNull Scope scope,
                                @NotNull List<UsageInfo> usages,
                                @NotNull PsiFile file,
                                @NotNull Editor editor,
                                @NotNull Project project) {
    Map<OccurrencesChooser.ReplaceChoice, List<Object>> occurrencesMap = getOccurrenceOptions(target, usages);
    OccurrencesChooser<Object> chooser = new OccurrencesChooser<Object>(editor) {
      @Override
      protected TextRange getOccurrenceRange(Object occurrence) {
        return IntroduceHandler.this.getOccurrenceRange(occurrence);
      }
    };
    chooser.showChooser(new Pass<OccurrencesChooser.ReplaceChoice>() {
      @Override
      public void pass(final OccurrencesChooser.ReplaceChoice choice) {
        AbstractInplaceIntroducer<?, ?> introducer = getIntroducer(target, scope, usages, choice, file, editor, project);
        introducer.startInplaceIntroduceTemplate();
      }
    }, occurrencesMap);
  }

  @NotNull
  private TextRange getOccurrenceRange(@NotNull Object occurrence) {
    if (occurrence instanceof PsiElement) {
      return ((PsiElement)occurrence).getTextRange();
    }
    else if (occurrence instanceof UsageInfo) {
      Segment segment = ((UsageInfo)occurrence).getSegment();
      assert segment != null;
      return TextRange.create(segment);
    }
    else {
      //assert occurrence instanceof Target;
      //noinspection unchecked
      return ((Target)occurrence).getTextRange();
    }
  }

  @NotNull
  private Map<OccurrencesChooser.ReplaceChoice, List<Object>> getOccurrenceOptions(@NotNull Target target,
                                                                                   @NotNull List<UsageInfo> usages) {
    HashMap<OccurrencesChooser.ReplaceChoice, List<Object>> map = ContainerUtil.newLinkedHashMap();

    map.put(OccurrencesChooser.ReplaceChoice.NO, Collections.singletonList(target));
    if (usages.size() > 1) {
      map.put(OccurrencesChooser.ReplaceChoice.ALL, ContainerUtil.newArrayList(usages));
    }
    return map;
  }


  @NotNull
  protected abstract List<UsageInfo> collectUsages(@NotNull Target target,
                                                   @NotNull Scope scope);

  /**
   * @return null if everything is ok, or a short message describing why it's impossible to perform the refactoring. It will be shown in a balloon popup.
   */
  @Nullable
  protected abstract String checkUsages(@NotNull List<UsageInfo> usages);

  /**
   * @return find all possible scopes for the target to introduce
   */
  @NotNull
  protected abstract List<Scope> collectTargetScopes(@NotNull Target target,
                                                     @NotNull Editor editor,
                                                     @NotNull PsiFile file,
                                                     @NotNull Project project);

  /**
   * @return candidates for refactoring (e.g. all expressions which are under caret)
   */
  @NotNull
  protected abstract Pair<List<Target>, Integer> collectTargets(@NotNull PsiFile file,
                                                                @NotNull Editor editor,
                                                                @NotNull Project project);

  /**
   *
   * @param start start offset of the selection
   * @param end end offset of the selection
   * @return the corresponding target, or null if the range doesn't match any target
   */
  @Nullable
  protected abstract Target findSelectionTarget(int start,
                                                int end,
                                                @NotNull PsiFile file,
                                                @NotNull Editor editor,
                                                @NotNull Project project);

  /**
   *
   * @param target to check
   * @return null if everything is ok, or a short message describing why the refactoring cannot be performed
   */
  @Nullable
  protected abstract String checkSelectedTarget(@NotNull Target target,
                                                @NotNull PsiFile file,
                                                @NotNull Editor editor,
                                                @NotNull Project project);

  @NotNull
  protected abstract String getRefactoringName();

  @Nullable
  protected abstract String getHelpID();

  /**
   * If {@link IntroduceHandler#collectTargetScopes}() returns several possible scopes, the Choose Scope Popup will be shown.
   * It will have this title.
   */
  @NotNull
  protected abstract String getChooseScopeTitle();

  /**
   * If {@link IntroduceHandler#collectTargetScopes}() returns several possible scopes, the Choose Scope Popup will be shown.
   * It will use the provided renderer to paint scopes
   */
  @NotNull
  protected abstract PsiElementListCellRenderer<Scope> getScopeRenderer();

  /**
   * @return in-place introducer for the refactoring
   */
  @NotNull
  protected abstract AbstractInplaceIntroducer<?, ?> getIntroducer(@NotNull Target target,
                                                                   @NotNull Scope scope,
                                                                   @NotNull List<UsageInfo> usages,
                                                                   @NotNull OccurrencesChooser.ReplaceChoice replaceChoice,
                                                                   @NotNull PsiFile file,
                                                                   @NotNull Editor editor,
                                                                   @NotNull Project project);

  @NotNull
  protected String getEmptyScopeErrorMessage() {
    return getRefactoringName() + " is not available in the current scope";
  }


  protected void showErrorHint(@NotNull String errorMessage, @NotNull Editor editor, @NotNull Project project) {
    CommonRefactoringUtil.showErrorHint(project, editor, errorMessage, getRefactoringName(), getHelpID());
  }

  private void cannotPerformRefactoring(@NotNull Project project, @NotNull Editor editor) {
    showErrorHint(RefactoringBundle.message("cannot.perform.refactoring"), editor, project);
  }
}

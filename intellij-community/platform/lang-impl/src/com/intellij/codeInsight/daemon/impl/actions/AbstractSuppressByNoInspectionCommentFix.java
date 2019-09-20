/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.codeInsight.daemon.impl.actions;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.SuppressIntentionAction;
import com.intellij.codeInspection.SuppressionUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author Roman.Chernyatchik
 * @date Aug 13, 2009
 * @deprecated use {@link AbstractBatchSuppressByNoInspectionCommentFix} and {@link com.intellij.codeInspection.SuppressIntentionActionFromFix}
 */
@Deprecated
public abstract class AbstractSuppressByNoInspectionCommentFix extends SuppressIntentionAction {
  @NotNull protected final String myID;
  private final boolean myReplaceOtherSuppressionIds;

  @Nullable
  protected abstract PsiElement getContainer(final PsiElement context);

  /**
   * @param ID                         Inspection ID
   * @param replaceOtherSuppressionIds Merge suppression policy. If false new tool id will be append to the end
   *                                   otherwise replace other ids
   */
  public AbstractSuppressByNoInspectionCommentFix(@NotNull String ID, final boolean replaceOtherSuppressionIds) {
    myID = ID;
    myReplaceOtherSuppressionIds = replaceOtherSuppressionIds;
  }

  protected final void replaceSuppressionComment(@NotNull final PsiElement comment) {
    SuppressionUtil.replaceSuppressionComment(comment, myID, myReplaceOtherSuppressionIds, getCommentLanguage(comment));
  }

  protected void createSuppression(@NotNull Project project,
                                   @NotNull PsiElement element,
                                   @NotNull PsiElement container) throws IncorrectOperationException {
    SuppressionUtil.createSuppression(project, container, myID, getCommentLanguage(element));
  }

  /**
   * @param element quickfix target or existing comment element
   * @return language that will be used for comment creating.
   * In common case language will be the same as language of quickfix target
   */
  @NotNull
  protected Language getCommentLanguage(@NotNull PsiElement element) {
    return element.getLanguage();
  }

  @Override
  public boolean isAvailable(@NotNull final Project project, final Editor editor, @NotNull final PsiElement context) {
    return context.isValid() && BaseIntentionAction.canModify(context) && getContainer(context) != null;
  }

  @Override
  public void invoke(@NotNull final Project project, @Nullable Editor editor, @NotNull final PsiElement element) throws IncorrectOperationException {
    PsiElement container = getContainer(element);
    if (container == null) return;

    final List<? extends PsiElement> comments = getCommentsFor(container);
    if (comments != null) {
      for (PsiElement comment : comments) {
        if (comment instanceof PsiComment && SuppressionUtil.isSuppressionComment(comment)) {
          replaceSuppressionComment(comment);
          return;
        }
      }
    }

    boolean caretWasBeforeStatement = editor != null && editor.getCaretModel().getOffset() == container.getTextRange().getStartOffset();
    try {
      createSuppression(project, element, container);
    }
    catch (IncorrectOperationException e) {
      if (!ApplicationManager.getApplication().isUnitTestMode() && editor != null) {
        Messages.showErrorDialog(editor.getComponent(),
                                 InspectionsBundle.message("suppress.inspection.annotation.syntax.error", e.getMessage()));
      }
    }

    if (caretWasBeforeStatement) {
      editor.getCaretModel().moveToOffset(container.getTextRange().getStartOffset());
    }
    UndoUtil.markPsiFileForUndo(element.getContainingFile());
  }

  @Nullable
  protected List<? extends PsiElement> getCommentsFor(@NotNull final PsiElement container) {
    final PsiElement prev = PsiTreeUtil.skipWhitespacesBackward(container);
    if (prev == null) {
      return null;
    }
    return Collections.singletonList(prev);
  }


  @Override
  @NotNull
  public String getFamilyName() {
    return InspectionsBundle.message("suppress.inspection.family");
  }
}

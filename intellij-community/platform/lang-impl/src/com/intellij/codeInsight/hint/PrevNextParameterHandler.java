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

package com.intellij.codeInsight.hint;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author ven
 */
public class PrevNextParameterHandler extends EditorActionHandler {
  public PrevNextParameterHandler(boolean isNextParameterHandler) {
    myIsNextParameterHandler = isNextParameterHandler;
  }

  private final boolean myIsNextParameterHandler;

  @Override
  protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
    if (!ParameterInfoController.existsForEditor(editor)) return false;

    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) return false;

    PsiElement exprList = getExpressionList(editor, caret.getOffset(), project);
    if (exprList == null) return false;

    int lbraceOffset = exprList.getTextRange().getStartOffset();
    return ParameterInfoController.findControllerAtOffset(editor, lbraceOffset) != null &&
           ParameterInfoController.hasPrevOrNextParameter(editor, lbraceOffset, myIsNextParameterHandler);
  }

  @Override
  protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
    int offset = caret != null ? caret.getOffset() : editor.getCaretModel().getOffset();
    PsiElement exprList = getExpressionList(editor, offset, dataContext);
    if (exprList != null) {
      int listOffset = exprList.getTextRange().getStartOffset();
      ParameterInfoController.prevOrNextParameter(editor, listOffset, myIsNextParameterHandler);
    }
  }

  @Nullable
  private static PsiElement getExpressionList(@NotNull Editor editor, int offset, DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    return project != null ? getExpressionList(editor, offset, project) : null;
  }

  @Nullable
  private static PsiElement getExpressionList(@NotNull Editor editor, int offset, @NotNull Project project) {
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    return file != null ? ParameterInfoController.findArgumentList(file, offset, -1) : null;
  }
}
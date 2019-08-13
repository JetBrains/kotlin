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
package com.intellij.codeInsight.editorActions.moveUpDown;

import com.intellij.lang.ASTNode;
import com.intellij.lang.DependentLanguage;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dennis.Ushakov
 */
public abstract class BaseMoveHandler extends EditorWriteActionHandler {
  protected final boolean isDown;

  public BaseMoveHandler(boolean down) {
    super(true);
    isDown = down;
  }

  @Override
  public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
    final Project project = editor.getProject();
    assert project != null;
    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    final Document document = editor.getDocument();
    documentManager.commitDocument(document);
    PsiFile file = getRoot(documentManager.getPsiFile(document), editor);

    if (file != null) {
      final MoverWrapper mover = getSuitableMover(editor, file);
      if (mover != null && mover.getInfo().toMove2 != null) {
        LineRange range = mover.getInfo().toMove;
        if ((range.startLine > 0 || isDown) && (range.endLine < document.getLineCount() || !isDown)) {
          mover.move(editor, file);
        }
      }
    }
  }

  @Override
  public boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
    if (editor.isViewer() || editor.isOneLineMode()) return false;
    final Project project = editor.getProject();
    if (project == null || project.isDisposed()) return false;
    return true;
  }

  @Nullable
  protected abstract MoverWrapper getSuitableMover(@NotNull Editor editor, @NotNull PsiFile file);

  @Nullable
  private static PsiFile getRoot(final PsiFile file, final Editor editor) {
    if (file == null) return null;
    int offset = editor.getCaretModel().getOffset();
    if (offset == editor.getDocument().getTextLength()) offset--;
    if (offset<0) return null;
    PsiElement leafElement = file.findElementAt(offset);
    if (leafElement == null) return null;
    if (leafElement.getLanguage() instanceof DependentLanguage) {
      leafElement = file.getViewProvider().findElementAt(offset, file.getViewProvider().getBaseLanguage());
      if (leafElement == null) return null;
    }
    ASTNode node = leafElement.getNode();
    if (node == null) return null;
    return (PsiFile)PsiUtilBase.getRoot(node).getPsi();
  }
}

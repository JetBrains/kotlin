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

package com.intellij.codeInsight.editorActions;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.editor.actions.CopyAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CutHandler extends EditorWriteActionHandler {
  private final EditorActionHandler myOriginalHandler;

  public CutHandler(EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Override
  public void executeWriteAction(final Editor editor, Caret caret, DataContext dataContext) {
    assert caret == null : "Invocation of 'cut' operation for specific caret is not supported";
    Project project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(editor.getContentComponent()));
    if (project == null) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, null, dataContext);
      }
      return;
    }

    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

    if (file == null) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, null, dataContext);
      }
      return;
    }

    final SelectionModel selectionModel = editor.getSelectionModel();
    if (!selectionModel.hasSelection(true)) {
      if (Registry.is(CopyAction.SKIP_COPY_AND_CUT_FOR_EMPTY_SELECTION_KEY)) {
        return;
      }
      editor.getCaretModel().runForEachCaret(__ -> selectionModel.selectLineAtCaret());
      if (!selectionModel.hasSelection(true)) return;
    }

    int start = selectionModel.getSelectionStart();
    int end = selectionModel.getSelectionEnd();
    final List<TextRange> selections = new ArrayList<>();
    if (editor.getCaretModel().supportsMultipleCarets()) {
      editor.getCaretModel().runForEachCaret(
        __ -> selections.add(new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd())));
    }

    EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_COPY).execute(editor, null, dataContext);

    if (editor.getCaretModel().supportsMultipleCarets()) {

      Collections.reverse(selections);
      final Iterator<TextRange> it = selections.iterator();
      editor.getCaretModel().runForEachCaret(__ -> {
        TextRange range = it.next();
        editor.getCaretModel().moveToOffset(range.getStartOffset());
        selectionModel.removeSelection();
        editor.getDocument().deleteString(range.getStartOffset(), range.getEndOffset());
      });
      editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    }
    else {
      if (start != end) {
        // There is a possible case that 'sticky selection' is active. It's automatically removed on copying then, so, we explicitly
        // remove the text.
        editor.getDocument().deleteString(start, end);
      }
      else {
        EditorModificationUtil.deleteSelectedText(editor);
      }
    }
  }
}

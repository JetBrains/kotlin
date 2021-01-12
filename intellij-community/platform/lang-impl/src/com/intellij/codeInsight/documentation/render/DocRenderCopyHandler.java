// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class DocRenderCopyHandler extends EditorActionHandler {
  private final EditorActionHandler myOriginalHandler;

  public DocRenderCopyHandler(EditorActionHandler handler) {myOriginalHandler = handler;}

  @Override
  protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
    if (!editor.getSelectionModel().hasSelection(true)) {
      DocRenderer.EditorPane pane = DocRenderSelectionManager.getPaneWithSelection(editor);
      if (pane != null) {
        String text = pane.getSelectedText();
        if (!StringUtil.isEmpty(text)) {
          Point selectionPositionInEditor = pane.getSelectionPositionInEditor();
          if (selectionPositionInEditor != null) {
            CopyPasteManager.getInstance().setContents(new StringSelection(text));

            ScrollingModel scrollingModel = editor.getScrollingModel();
            if (!scrollingModel.getVisibleAreaOnScrollingFinished().contains(selectionPositionInEditor)) {
              scrollingModel.scroll(0, selectionPositionInEditor.y - scrollingModel.getVisibleArea().height / 3);
            }

            return;
          }
        }
      }
    }
    myOriginalHandler.execute(editor, caret, dataContext);
  }
}

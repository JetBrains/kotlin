// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import org.jetbrains.annotations.NotNull;

class DocRenderSelectionManager implements CaretListener, SelectionListener {
  private final Editor myEditor;
  private DocRenderer.EditorPane myPaneWithSelection;
  private boolean mySkipSelectionEvents;

  DocRenderSelectionManager(Editor editor) {myEditor = editor;}

  void setPaneWithSelection(DocRenderer.EditorPane pane) {
    if (pane != myPaneWithSelection) {
      if (myPaneWithSelection != null) {
        myPaneWithSelection.removeSelection();
      }
      myPaneWithSelection = pane;
      if (myPaneWithSelection != null) {
        mySkipSelectionEvents = true;
        try {
          myEditor.getSelectionModel().removeSelection(true);
        }
        finally {
          mySkipSelectionEvents = false;
        }
      }
    }
  }

  @Override
  public void caretPositionChanged(@NotNull CaretEvent event) {
    setPaneWithSelection(null);
  }

  @Override
  public void caretAdded(@NotNull CaretEvent event) {
    setPaneWithSelection(null);
  }

  @Override
  public void caretRemoved(@NotNull CaretEvent event) {
    setPaneWithSelection(null);
  }

  @Override
  public void selectionChanged(@NotNull SelectionEvent e) {
    if (!mySkipSelectionEvents) setPaneWithSelection(null);
  }
}

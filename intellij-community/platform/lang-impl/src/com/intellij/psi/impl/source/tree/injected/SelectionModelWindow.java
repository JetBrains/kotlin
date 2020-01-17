// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class SelectionModelWindow implements SelectionModel {
  private final SelectionModel myHostModel;
  private final DocumentWindow myDocument;
  private final EditorWindow myInjectedEditor;

  SelectionModelWindow(final EditorEx delegate, final DocumentWindow document, EditorWindow injectedEditor) {
    myDocument = document;
    myInjectedEditor = injectedEditor;
    myHostModel = delegate.getSelectionModel();
  }

  @Override
  public @NotNull Editor getEditor() {
    return myInjectedEditor;
  }

  @Nullable
  @Override
  public VisualPosition getSelectionStartPosition() {
    return myInjectedEditor.offsetToVisualPosition(getSelectionStart());
  }

  @Nullable
  @Override
  public VisualPosition getSelectionEndPosition() {
    return myInjectedEditor.offsetToVisualPosition(getSelectionEnd());
  }

  @Override
  public boolean hasSelection(boolean anyCaret) {
    return myHostModel.hasSelection(anyCaret);
  }

  @Override
  public void addSelectionListener(@NotNull final SelectionListener listener) {
    myHostModel.addSelectionListener(listener);
  }

  @Override
  public void removeSelectionListener(@NotNull final SelectionListener listener) {
    myHostModel.removeSelectionListener(listener);
  }

  @Override
  public void copySelectionToClipboard() {
    myHostModel.copySelectionToClipboard();
  }

  @Override
  public void setBlockSelection(@NotNull final LogicalPosition blockStart, @NotNull final LogicalPosition blockEnd) {
    myHostModel.setBlockSelection(myInjectedEditor.injectedToHost(blockStart), myInjectedEditor.injectedToHost(blockEnd));
  }

  @Override
  public int @NotNull [] getBlockSelectionStarts() {
    int[] result = myHostModel.getBlockSelectionStarts();
    for (int i = 0; i < result.length; i++) {
      result[i] = myDocument.hostToInjected(result[i]);
    }
    return result;
  }

  @Override
  public int @NotNull [] getBlockSelectionEnds() {
    int[] result = myHostModel.getBlockSelectionEnds();
    for (int i = 0; i < result.length; i++) {
      result[i] = myDocument.hostToInjected(result[i]);
    }
    return result;
  }

  @Override
  public TextAttributes getTextAttributes() {
    return myHostModel.getTextAttributes();
  }
}

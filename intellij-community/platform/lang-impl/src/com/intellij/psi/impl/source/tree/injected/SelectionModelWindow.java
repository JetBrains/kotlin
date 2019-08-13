// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
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
  public int getSelectionStart() {
    return myDocument.hostToInjected(myHostModel.getSelectionStart());
  }

  @Nullable
  @Override
  public VisualPosition getSelectionStartPosition() {
    return myInjectedEditor.offsetToVisualPosition(getSelectionStart());
  }

  @Override
  public int getSelectionEnd() {
    return myDocument.hostToInjected(myHostModel.getSelectionEnd());
  }

  @Nullable
  @Override
  public VisualPosition getSelectionEndPosition() {
    return myInjectedEditor.offsetToVisualPosition(getSelectionEnd());
  }

  @Override
  public String getSelectedText() {
    return myHostModel.getSelectedText();
  }

  @Nullable
  @Override
  public String getSelectedText(boolean allCarets) {
    return myHostModel.getSelectedText(allCarets);
  }

  @Override
  public int getLeadSelectionOffset() {
    return myDocument.hostToInjected(myHostModel.getLeadSelectionOffset());
  }

  @Nullable
  @Override
  public VisualPosition getLeadSelectionPosition() {
    return myHostModel.getLeadSelectionPosition();
  }

  @Override
  public boolean hasSelection() {
    return myHostModel.hasSelection();
  }

  @Override
  public boolean hasSelection(boolean anyCaret) {
    return myHostModel.hasSelection(anyCaret);
  }

  @Override
  public void setSelection(final int startOffset, final int endOffset) {
    TextRange hostRange = myDocument.injectedToHost(new ProperTextRange(startOffset, endOffset));
    myHostModel.setSelection(hostRange.getStartOffset(), hostRange.getEndOffset());
  }

  @Override
  public void setSelection(int startOffset, @Nullable VisualPosition endPosition, int endOffset) {
    TextRange hostRange = myDocument.injectedToHost(new ProperTextRange(startOffset, endOffset));
    myHostModel.setSelection(hostRange.getStartOffset(), endPosition, hostRange.getEndOffset());
  }

  @Override
  public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset) {
    TextRange hostRange = myDocument.injectedToHost(new ProperTextRange(startOffset, endOffset));
    myHostModel.setSelection(startPosition, hostRange.getStartOffset(), endPosition, hostRange.getEndOffset());
  }

  @Override
  public void removeSelection() {
    myHostModel.removeSelection();
  }

  @Override
  public void removeSelection(boolean allCarets) {
    myHostModel.removeSelection(allCarets);
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
  public void selectLineAtCaret() {
    myHostModel.selectLineAtCaret();
  }

  @Override
  public void selectWordAtCaret(final boolean honorCamelWordsSettings) {
    myHostModel.selectWordAtCaret(honorCamelWordsSettings);
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
  @NotNull
  public int[] getBlockSelectionStarts() {
    int[] result = myHostModel.getBlockSelectionStarts();
    for (int i = 0; i < result.length; i++) {
      result[i] = myDocument.hostToInjected(result[i]);
    }
    return result;
  }

  @Override
  @NotNull
  public int[] getBlockSelectionEnds() {
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

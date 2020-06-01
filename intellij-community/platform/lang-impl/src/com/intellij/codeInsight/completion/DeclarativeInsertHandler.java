// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

public final class DeclarativeInsertHandler implements InsertHandler<LookupElement> {
  public final String ignoredCompletionChars;
  public final String valueToInsert;
  public final boolean autoPopup;

  private DeclarativeInsertHandler(@NotNull String ignoredCompletionChars, @NotNull String valueToInsert, boolean autoPopup) {
    this.ignoredCompletionChars = ignoredCompletionChars;
    this.valueToInsert = valueToInsert;
    this.autoPopup = autoPopup;
  }

  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    char completionChar = context.getCompletionChar();
    if (StringUtil.containsChar(ignoredCompletionChars, completionChar)) return;
    Project project = editor.getProject();
    if (project != null) {
      CaretModel model = editor.getCaretModel();
      if (isValueAlreadyHere(editor)) {
        model.moveToOffset(model.getOffset() + valueToInsert.length());
      }
      else {
        EditorModificationUtil.insertStringAtCaret(editor, valueToInsert);
        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
      }
      if (autoPopup) {
        AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, null);
      }
    }
  }

  private boolean isValueAlreadyHere(@NotNull Editor editor) {
    int startOffset = editor.getCaretModel().getOffset();
    Document document = editor.getDocument();
    int valueLength = valueToInsert.length();
    return document.getTextLength() >= startOffset + valueLength &&
           document.getText(TextRange.create(startOffset, startOffset + valueLength)).equals(valueToInsert);
  }

  public final static class Builder {
    private String myIgnoredCharacters;
    private String myValueToInsert;
    private boolean myTriggerAutoPopup;

    @NotNull
    public DeclarativeInsertHandler.Builder disableOnCompletionChars(@NotNull String ignoredChars) {
      myIgnoredCharacters = ignoredChars;
      return this;
    }

    @NotNull
    public DeclarativeInsertHandler.Builder insertOrMove(@NotNull String value) {
      myValueToInsert = value;
      return this;
    }

    @NotNull
    public DeclarativeInsertHandler.Builder triggerAutoPopup() {
      myTriggerAutoPopup = true;
      return this;
    }

    @NotNull
    public DeclarativeInsertHandler build() {
      return new DeclarativeInsertHandler(StringUtil.notNullize(myIgnoredCharacters),
                                          StringUtil.notNullize(myValueToInsert),
                                          myTriggerAutoPopup);
    }
  }
}

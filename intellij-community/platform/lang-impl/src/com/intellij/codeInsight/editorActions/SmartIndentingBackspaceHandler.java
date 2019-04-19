// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeStyle.CodeStyleFacade;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SmartIndentingBackspaceHandler extends AbstractIndentingBackspaceHandler {
  private static final Logger LOG = Logger.getInstance(SmartIndentingBackspaceHandler.class);

  private String myReplacement;
  private int myStartOffset;

  public SmartIndentingBackspaceHandler() {
    super(SmartBackspaceMode.AUTOINDENT);
  }

  @Override
  protected void doBeforeCharDeleted(char c, PsiFile file, Editor editor) {
    Project project = file.getProject();
    Document document = editor.getDocument();
    CharSequence charSequence = document.getImmutableCharSequence();
    CaretModel caretModel = editor.getCaretModel();
    int caretOffset = caretModel.getOffset();
    LogicalPosition pos = caretModel.getLogicalPosition();
    int lineStartOffset = document.getLineStartOffset(pos.line);
    int beforeWhitespaceOffset = CharArrayUtil.shiftBackward(charSequence, caretOffset - 1, " \t") + 1;
    if (beforeWhitespaceOffset != lineStartOffset) {
      myReplacement = null;
      return;
    }
    CodeStyleFacade codeStyleFacade = CodeStyleFacade.getInstance(project);
    myReplacement = codeStyleFacade.getLineIndent(editor, file.getLanguage(), lineStartOffset, true);
    if (myReplacement == null) {
      return;
    }
    int tabSize = CodeStyle.getIndentOptions(file).TAB_SIZE;
    int targetColumn = getWidth(myReplacement, tabSize);
    int endOffset = CharArrayUtil.shiftForward(charSequence, caretOffset, " \t");
    LogicalPosition logicalPosition = caretOffset < endOffset ? editor.offsetToLogicalPosition(endOffset) : pos;
    int currentColumn = logicalPosition.column;
    if (currentColumn > targetColumn) {
      myStartOffset = lineStartOffset;
    }
    else if (logicalPosition.line == 0) {
      myStartOffset = 0;
      myReplacement = "";
    }
    else {
      int prevLineEndOffset = document.getLineEndOffset(logicalPosition.line - 1);
      myStartOffset = CharArrayUtil.shiftBackward(charSequence, prevLineEndOffset - 1, " \t") + 1;
      if (myStartOffset != document.getLineStartOffset(logicalPosition.line - 1)) {
        int spacing = codeStyleFacade.getJoinedLinesSpacing(editor, file.getLanguage(), endOffset, true);
        if (spacing < 0) {
          LOG.error("The call `codeStyleFacade.getJoinedLinesSpacing` should not return the negative value");
          spacing = 0;
        }
        myReplacement = StringUtil.repeatSymbol(' ', spacing);
      }
    }
  }

  @Override
  protected boolean doCharDeleted(char c, PsiFile file, Editor editor) {
    if (myReplacement == null) {
      return false;
    }

    Document document = editor.getDocument();
    CaretModel caretModel = editor.getCaretModel();
    int endOffset = CharArrayUtil.shiftForward(document.getImmutableCharSequence(), caretModel.getOffset(), " \t");

    if (editor instanceof EditorWindow) {
      List<TextRange> ranges = InjectedLanguageManager.getInstance(file.getProject())
                                                      .intersectWithAllEditableFragments(file, new TextRange(myStartOffset, endOffset));
      if (ranges.size() != 1 || !ranges.get(0).equalsToRange(myStartOffset, endOffset)) return false;
    }

    document.replaceString(myStartOffset, endOffset, myReplacement);
    caretModel.moveToOffset(myStartOffset + myReplacement.length());

    return true;
  }

  private static int getWidth(@NotNull String indent, int tabSize) {
    int width = 0;
    for (int i = 0; i < indent.length(); i++) {
      char c = indent.charAt(i);
      switch (c) {
        case '\t':
          width = tabSize * (width / tabSize + 1);
          break;
        default:
          LOG.error("Unexpected whitespace character: " + ((int)c));
        case ' ':
          width++;
      }
    }
    return width;
  }
}

// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RawText;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.util.DocumentUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LineCommentCopyPastePreProcessor implements CopyPastePreProcessor {
  @Nullable
  @Override
  public String preprocessOnCopy(PsiFile file, int[] startOffsets, int[] endOffsets, String text) {
    return null;
  }

  @NotNull
  @Override
  public String preprocessOnPaste(Project project, PsiFile file, Editor editor, String text, RawText rawText) {
    Language language = file.getLanguage();
    Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(language);
    if (commenter == null) return text;
    String lineCommentPrefix = commenter.getLineCommentPrefix();
    if (lineCommentPrefix == null) return text;

    Document document = editor.getDocument();
    int offset = editor.getSelectionModel().getSelectionStart();
    if (DocumentUtil.isAtLineEnd(offset, editor.getDocument()) && text.startsWith("\n")) return text;

    int lineStartOffset = DocumentUtil.getLineStartOffset(offset, document);
    CharSequence chars = document.getImmutableCharSequence();
    int firstNonWsLineOffset = CharArrayUtil.shiftForward(chars, lineStartOffset, " \t");
    if (offset < (firstNonWsLineOffset + lineCommentPrefix.length()) || 
        !CharArrayUtil.regionMatches(chars, firstNonWsLineOffset, lineCommentPrefix)) return text;
    
    CodeStyleSettings codeStyleSettings = CodeStyle.getSettings(file);
    String lineStartReplacement = "\n" + chars.subSequence(lineStartOffset, firstNonWsLineOffset + lineCommentPrefix.length()) +
                                  (codeStyleSettings.getCommonSettings(language).LINE_COMMENT_ADD_SPACE ? " " : "");
    return StringUtil.trimTrailing(text, '\n').replace("\n", lineStartReplacement);
  }
}

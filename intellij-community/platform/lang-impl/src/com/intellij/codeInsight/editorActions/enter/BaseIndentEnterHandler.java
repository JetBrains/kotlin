/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.codeInsight.editorActions.enter;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageFormatting;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by IntelliJ IDEA.
 *
 * @author oleg
 * @date 11/17/10
 */
public class BaseIndentEnterHandler extends EnterHandlerDelegateAdapter {
  private final Language myLanguage;
  private final TokenSet myIndentTokens;
  private final IElementType myLineCommentType;
  private final String myLineCommentPrefix;
  private final TokenSet myWhitespaceTokens;
  private final boolean myWorksWithFormatter;

  public BaseIndentEnterHandler(
    final Language language,
    final TokenSet indentTokens,
    final IElementType lineCommentType,
    final String lineCommentPrefix,
    final TokenSet whitespaceTokens)
  {
    this(language, indentTokens, lineCommentType, lineCommentPrefix, whitespaceTokens, false);
  }


  public BaseIndentEnterHandler(
    final Language language,
    final TokenSet indentTokens,
    final IElementType lineCommentType,
    final String lineCommentPrefix,
    final TokenSet whitespaceTokens,
    final boolean worksWithFormatter)
  {
    myLanguage = language;
    myIndentTokens = indentTokens;
    myLineCommentType = lineCommentType;
    myLineCommentPrefix = lineCommentPrefix;
    myWhitespaceTokens = whitespaceTokens;
    myWorksWithFormatter = worksWithFormatter;
  }

  protected Result shouldSkipWithResult(@NotNull final PsiFile file, @NotNull final Editor editor, @NotNull final DataContext dataContext) {
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return Result.Continue;
    }

    if (!file.getViewProvider().getLanguages().contains(myLanguage)) {
      return Result.Continue;
    }

    if (editor.isViewer()) {
      return Result.Continue;
    }

    final Document document = editor.getDocument();
    if (!document.isWritable()) {
      return Result.Continue;
    }

    PsiDocumentManager.getInstance(project).commitDocument(document);

    int caret = editor.getCaretModel().getOffset();
    if (caret == 0) {
      return Result.DefaultSkipIndent;
    }
    if (caret <= 0) {
      return Result.Continue;
    }
    return null;
  }

  @Override
  public Result preprocessEnter(
    @NotNull final PsiFile file,
    @NotNull final Editor editor,
    @NotNull final Ref<Integer> caretOffset,
    @NotNull final Ref<Integer> caretAdvance,
    @NotNull final DataContext dataContext,
    final EditorActionHandler originalHandler)
  {
    Result res = shouldSkipWithResult(file, editor, dataContext);
    if (res != null) {
      return res;
    }

    final Document document = editor.getDocument();
    int caret = editor.getCaretModel().getOffset();
    final int lineNumber = document.getLineNumber(caret);

    final int lineStartOffset = document.getLineStartOffset(lineNumber);
    final int previousLineStartOffset = lineNumber > 0 ? document.getLineStartOffset(lineNumber - 1) : lineStartOffset;
    final EditorHighlighter highlighter = ((EditorEx)editor).getHighlighter();
    final HighlighterIterator iterator = highlighter.createIterator(caret - 1);
    final IElementType type = getNonWhitespaceElementType(iterator, lineStartOffset, previousLineStartOffset);

    final CharSequence editorCharSequence = document.getCharsSequence();
    final CharSequence lineIndent =
      editorCharSequence.subSequence(lineStartOffset, EditorActionUtil.findFirstNonSpaceOffsetOnTheLine(document, lineNumber));

    // Enter in line comment
    if (type == myLineCommentType) {
      final String restString = editorCharSequence.subSequence(caret, document.getLineEndOffset(lineNumber)).toString();
      if (!StringUtil.isEmptyOrSpaces(restString)) {
        final String linePrefix = lineIndent + myLineCommentPrefix;
        EditorModificationUtil.insertStringAtCaret(editor, "\n" + linePrefix);
        editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(lineNumber + 1, linePrefix.length()));
        return Result.Stop;
      }
      else if (iterator.getStart() < lineStartOffset) {
        EditorModificationUtil.insertStringAtCaret(editor, "\n" + lineIndent);
        return Result.Stop;
      }
    }

    if (!myWorksWithFormatter && LanguageFormatting.INSTANCE.forLanguage(myLanguage) != null) {
      return Result.Continue;
    }
    else {
      if (myIndentTokens.contains(type)) {
        final String newIndent = getNewIndent(file, document, lineIndent);
        EditorModificationUtil.insertStringAtCaret(editor, "\n" + newIndent);
        return Result.Stop;
      }

      EditorModificationUtil.insertStringAtCaret(editor, "\n" + lineIndent);
      editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(lineNumber + 1, calcLogicalLength(editor, lineIndent)));
      return Result.Stop;
    }
  }

  protected String getNewIndent(
    @NotNull final PsiFile file,
    @NotNull final Document document,
    @NotNull final CharSequence oldIndent)
  {
    CharSequence nonEmptyIndent = oldIndent;
    final CharSequence editorCharSequence = document.getCharsSequence();
    final int nLines = document.getLineCount();
    for (int line = 0; line < nLines && nonEmptyIndent.length() == 0; ++line) {
      final int lineStart = document.getLineStartOffset(line);
      final int indentEnd = EditorActionUtil.findFirstNonSpaceOffsetOnTheLine(document, line);
      if (lineStart < indentEnd) {
        nonEmptyIndent = editorCharSequence.subSequence(lineStart, indentEnd);
      }
    }

    final boolean usesSpacesForIndentation = nonEmptyIndent.length() > 0 && nonEmptyIndent.charAt(nonEmptyIndent.length() - 1) == ' ';
    final boolean firstIndent = nonEmptyIndent.length() == 0;

    final CodeStyleSettings currentSettings = CodeStyle.getSettings(file);
    final CommonCodeStyleSettings.IndentOptions indentOptions = currentSettings.getIndentOptions(file.getFileType());
    if (firstIndent && indentOptions.USE_TAB_CHARACTER || !firstIndent && !usesSpacesForIndentation) {
      int nTabsToIndent = indentOptions.INDENT_SIZE / indentOptions.TAB_SIZE;
      if (indentOptions.INDENT_SIZE % indentOptions.TAB_SIZE != 0) {
        ++nTabsToIndent;
      }
      return oldIndent + StringUtil.repeatSymbol('\t', nTabsToIndent);
    }
    return oldIndent + StringUtil.repeatSymbol(' ', indentOptions.INDENT_SIZE);
  }

  private static int calcLogicalLength(Editor editor, CharSequence lineIndent) {
    int result = 0;
    for (int i = 0; i < lineIndent.length(); i++) {
      if (lineIndent.charAt(i) == '\t') {
        result += EditorUtil.getTabSize(editor);
      } else {
        result++;
      }
    }
    return result;
  }

  @Nullable
  protected IElementType getNonWhitespaceElementType(final HighlighterIterator iterator, int currentLineStartOffset, final int prevLineStartOffset) {
    while (!iterator.atEnd() && iterator.getEnd() >= currentLineStartOffset && iterator.getStart() >= prevLineStartOffset) {
      final IElementType tokenType = iterator.getTokenType();
      if (!myWhitespaceTokens.contains(tokenType)) {
        return tokenType;
      }
      iterator.retreat();
    }
    return null;
  }
}


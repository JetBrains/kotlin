/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.codeInsight.generation;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.CommentUtil;
import com.intellij.codeInsight.actions.MultiCaretCodeInsightActionHandler;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.formatting.IndentData;
import com.intellij.ide.highlighter.custom.CustomFileTypeLexer;
import com.intellij.lang.*;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.AbstractFileType;
import com.intellij.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.templateLanguages.MultipleLangCommentProvider;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.LightweightHint;
import com.intellij.util.containers.IntArrayList;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CommentByBlockCommentHandler extends MultiCaretCodeInsightActionHandler {
  private Project myProject;
  private Editor myEditor;
  private Caret myCaret;
  private @NotNull PsiFile myFile;
  private Document myDocument;
  private Commenter myCommenter;
  private CommenterDataHolder mySelfManagedCommenterData;
  private String myWarning;
  private RangeMarker myWarningLocation;

  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Caret caret, @NotNull PsiFile file) {
    myProject = project;
    myEditor = editor;
    myCaret = caret;
    myFile = file;
    myWarning = null;
    myWarningLocation = null;

    myDocument = editor.getDocument();

    FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.comment.block");
    final Commenter commenter = findCommenter(myFile, myEditor, caret);
    if (commenter == null) return;
    myCommenter = commenter;

    final String prefix;
    final String suffix;

    if (commenter instanceof SelfManagingCommenter) {
      final SelfManagingCommenter selfManagingCommenter = (SelfManagingCommenter)commenter;
      mySelfManagedCommenterData = selfManagingCommenter.createBlockCommentingState(
        caret.getSelectionStart(),
        caret.getSelectionEnd(),
        myDocument,
        myFile
      );

      if (mySelfManagedCommenterData == null) {
        mySelfManagedCommenterData = SelfManagingCommenter.EMPTY_STATE;
      }

      prefix = selfManagingCommenter.getBlockCommentPrefix(
        caret.getSelectionStart(),
        myDocument,
        mySelfManagedCommenterData
      );
      suffix = selfManagingCommenter.getBlockCommentSuffix(
        caret.getSelectionEnd(),
        myDocument,
        mySelfManagedCommenterData
      );
    }
    else {
      prefix = commenter.getBlockCommentPrefix();
      suffix = commenter.getBlockCommentSuffix();
    }

    if (prefix == null || suffix == null) return;

    TextRange commentedRange = findCommentedRange(commenter);
    if (commentedRange != null) {
      final int commentStart = commentedRange.getStartOffset();
      final int commentEnd = commentedRange.getEndOffset();
      int selectionStart = commentStart;
      int selectionEnd = commentEnd;
      if (myCaret.hasSelection()) {
        selectionStart = myCaret.getSelectionStart();
        selectionEnd = myCaret.getSelectionEnd();
      }
      if ((commentStart < selectionStart || commentStart >= selectionEnd) && (commentEnd <= selectionStart || commentEnd > selectionEnd)) {
        commentRange(selectionStart, selectionEnd, prefix, suffix, commenter);
      }
      else {
        uncommentRange(commentedRange, trim(prefix), trim(suffix), commenter);
      }
    }
    else {
      if (myCaret.hasSelection()) {
        int selectionStart = myCaret.getSelectionStart();
        int selectionEnd = myCaret.getSelectionEnd();
        if (commenter instanceof IndentedCommenter) {
          final Boolean value = ((IndentedCommenter)commenter).forceIndentedBlockComment();
          if (value == Boolean.TRUE) {
            selectionStart = myDocument.getLineStartOffset(myDocument.getLineNumber(selectionStart));
            selectionEnd = myDocument.getLineEndOffset(myDocument.getLineNumber(selectionEnd));
          }
        }
        commentRange(selectionStart, selectionEnd, prefix, suffix, commenter);
      }
      else {
        EditorUtil.fillVirtualSpaceUntilCaret(editor);
        int caretOffset = myCaret.getOffset();
        if (commenter instanceof IndentedCommenter) {
          final Boolean value = ((IndentedCommenter)commenter).forceIndentedBlockComment();
          if (value == Boolean.TRUE) {
            final int lineNumber = myDocument.getLineNumber(caretOffset);
            final int start = myDocument.getLineStartOffset(lineNumber);
            final int end = myDocument.getLineEndOffset(lineNumber);
            commentRange(start, end, prefix, suffix, commenter);
            return;
          }
        }
        myDocument.insertString(caretOffset, prefix + suffix);
        myCaret.moveToOffset(caretOffset + prefix.length());
      }
    }

    showMessageIfNeeded();
  }

  private void showMessageIfNeeded() {
    if (myWarning != null) {
      myEditor.getScrollingModel().disableAnimation();
      myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
      myEditor.getScrollingModel().enableAnimation();
      
      LogicalPosition hintPosition = myCaret.getLogicalPosition();
      if (myWarningLocation != null) {
        LogicalPosition targetPosition = myEditor.offsetToLogicalPosition(myWarningLocation.getStartOffset());
        Point targetPoint = myEditor.logicalPositionToXY(targetPosition);
        if (myEditor.getScrollingModel().getVisibleArea().contains(targetPoint)) {
          hintPosition = targetPosition;
        }
      }
      LightweightHint hint = new LightweightHint(HintUtil.createInformationLabel(myWarning));
      Point p = HintManagerImpl.getHintPosition(hint, myEditor, hintPosition, HintManager.ABOVE);
      HintManagerImpl.getInstanceImpl().showEditorHint(hint, myEditor, p, 0, 0, false);
    }
  }

  @Nullable
  private static String trim(String s) {
    return s == null ? null : s.trim();
  }

  private boolean testSelectionForNonComments() {
    if (!myCaret.hasSelection()) {
      return true;
    }
    TextRange range
      = new TextRange(myCaret.getSelectionStart(), myCaret.getSelectionEnd() - 1);
    for (PsiElement element = myFile.findElementAt(range.getStartOffset()); element != null && range.intersects(element.getTextRange());
         element = element.getNextSibling()) {
      if (element instanceof OuterLanguageElement) {
        if (!isInjectedWhiteSpace(range, (OuterLanguageElement)element)) {
          return false;
        }
      }
      else {
        if (!isWhiteSpaceOrComment(element, range)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean isInjectedWhiteSpace(@NotNull TextRange range, @NotNull OuterLanguageElement element) {
    PsiElement psi = element.getContainingFile().getViewProvider().getPsi(element.getLanguage());
    if (psi == null) {
      return false;
    }
    List<PsiElement> injectedElements = PsiTreeUtil.getInjectedElements(element);
    for (PsiElement el : injectedElements) {
      if (!isWhiteSpaceOrComment(el, range)) {
        return false;
      }
    }
    return true;
  }

  private boolean isWhiteSpaceOrComment(@NotNull PsiElement element, @NotNull TextRange range) {
    final TextRange textRange = element.getTextRange();
    TextRange intersection = range.intersection(textRange);
    if (intersection == null) {
      return false;
    }
    intersection = TextRange.create(Math.max(intersection.getStartOffset() - textRange.getStartOffset(), 0),
                                    Math.min(intersection.getEndOffset() - textRange.getStartOffset(), textRange.getLength()));
    return isWhiteSpaceOrComment(element) ||
           intersection.substring(element.getText()).trim().isEmpty();
  }

  private static boolean isWhiteSpaceOrComment(PsiElement element) {
    return element instanceof PsiWhiteSpace ||
           PsiTreeUtil.getParentOfType(element, PsiComment.class, false) != null;
  }

  @Nullable
  private TextRange findCommentedRange(final Commenter commenter) {
    final CharSequence text = myDocument.getCharsSequence();
    final FileType fileType = myFile.getFileType();
    if (fileType instanceof CustomSyntaxTableFileType) {
      Lexer lexer = new CustomFileTypeLexer(((CustomSyntaxTableFileType)fileType).getSyntaxTable());
      final int caretOffset = myCaret.getOffset();
      int commentStart = CharArrayUtil.lastIndexOf(text, commenter.getBlockCommentPrefix(), caretOffset);
      if (commentStart == -1) return null;

      lexer.start(text, commentStart, text.length());
      if (lexer.getTokenType() == CustomHighlighterTokenType.MULTI_LINE_COMMENT && lexer.getTokenEnd() >= caretOffset) {
        return new TextRange(commentStart, lexer.getTokenEnd());
      }
      return null;
    }

    final String prefix;
    final String suffix;
    // Custom uncommenter is able to find commented block inside of selected text
    final String selectedText = myCaret.getSelectedText();
    if ((commenter instanceof CustomUncommenter) && selectedText != null) {
      final TextRange commentedRange = ((CustomUncommenter)commenter).findMaximumCommentedRange(selectedText);
      if (commentedRange == null) {
        return null;
      }
      // Uncommenter returns range relative to text start, so we need to shift it to make abosolute.
      return commentedRange.shiftRight(myCaret.getSelectionStart());
    }

    if (commenter instanceof SelfManagingCommenter) {
      SelfManagingCommenter selfManagingCommenter = (SelfManagingCommenter)commenter;

      prefix = selfManagingCommenter.getBlockCommentPrefix(
        myCaret.getSelectionStart(),
        myDocument,
        mySelfManagedCommenterData
      );
      suffix = selfManagingCommenter.getBlockCommentSuffix(
        myCaret.getSelectionEnd(),
        myDocument,
        mySelfManagedCommenterData
      );
    }
    else {
      prefix = trim(commenter.getBlockCommentPrefix());
      suffix = trim(commenter.getBlockCommentSuffix());
    }
    if (prefix == null || suffix == null) return null;

    TextRange commentedRange;

    if (commenter instanceof SelfManagingCommenter) {
      commentedRange = ((SelfManagingCommenter)commenter).getBlockCommentRange(
        myCaret.getSelectionStart(),
        myCaret.getSelectionEnd(),
        myDocument,
        mySelfManagedCommenterData
      );
    }
    else {
      if (!testSelectionForNonComments()) {
        return null;
      }

      commentedRange = getSelectedComments(text, prefix, suffix);
    }
    if (commentedRange == null) {
      PsiElement comment = findCommentAtCaret();
      if (comment != null) {

        String commentText = comment.getText();
        if (commentText.startsWith(prefix) && commentText.endsWith(suffix)) {
          commentedRange = comment.getTextRange();
        }
      }
    }
    return commentedRange;
  }


  @Nullable
  private TextRange getSelectedComments(CharSequence text, String prefix, String suffix) {
    TextRange commentedRange = null;
    if (myCaret.hasSelection()) {
      int selectionStart = myCaret.getSelectionStart();
      selectionStart = CharArrayUtil.shiftForward(text, selectionStart, " \t\n");
      int selectionEnd = myCaret.getSelectionEnd() - 1;
      selectionEnd = CharArrayUtil.shiftBackward(text, selectionEnd, " \t\n") + 1;
      if (selectionEnd - selectionStart >= prefix.length() + suffix.length() &&
          CharArrayUtil.regionMatches(text, selectionStart, prefix) &&
          CharArrayUtil.regionMatches(text, selectionEnd - suffix.length(), suffix)) {
        commentedRange = new TextRange(selectionStart, selectionEnd);
      }
    }
    return commentedRange;
  }

  @Nullable
  private static Commenter findCommenter(PsiFile file, Editor editor, Caret caret) {
    final FileType fileType = file.getFileType();
    if (fileType instanceof AbstractFileType) {
      return ((AbstractFileType)fileType).getCommenter();
    }

    Language lang = PsiUtilBase.getLanguageInEditor(caret, file.getProject());

    return getCommenter(file, editor, lang, lang);
  }

  @Nullable
  public static Commenter getCommenter(final PsiFile file, final Editor editor,
                                       final Language lineStartLanguage, final Language lineEndLanguage) {

    final FileViewProvider viewProvider = file.getViewProvider();

    for (MultipleLangCommentProvider provider : MultipleLangCommentProvider.EP_NAME.getExtensions()) {
      if (provider.canProcess(file, viewProvider)) {
        return provider.getLineCommenter(file, editor, lineStartLanguage, lineEndLanguage);
      }
    }

    final Language fileLanguage = file.getLanguage();
    Language lang = lineStartLanguage == null || LanguageCommenters.INSTANCE.forLanguage(lineStartLanguage) == null ||
                    fileLanguage.getBaseLanguage() == lineStartLanguage // file language is a more specific dialect of the line language
                    ? fileLanguage
                    : lineStartLanguage;

    if (viewProvider instanceof TemplateLanguageFileViewProvider &&
        lang == ((TemplateLanguageFileViewProvider)viewProvider).getTemplateDataLanguage()) {
      lang = viewProvider.getBaseLanguage();
    }

    return LanguageCommenters.INSTANCE.forLanguage(lang);
  }

  @Nullable
  private PsiElement findCommentAtCaret() {
    int offset = myCaret.getOffset();
    TextRange range = new TextRange(myCaret.getSelectionStart(), myCaret.getSelectionEnd());
    if (offset == range.getEndOffset()) {
      offset--;
    }
    if (offset <= range.getStartOffset()) {
      offset++;
    }
    PsiElement comment = getCommentAtOffset(offset);
    if (comment == null || myCaret.hasSelection() && !range.contains(comment.getTextRange())) {
      return null;
    }

    return comment;
  }

  @Nullable
  private PsiComment getCommentAtOffset(int offset) {
    PsiElement elt = myFile.getViewProvider().findElementAt(offset);
    if (elt == null) return null;
    return PsiTreeUtil.getParentOfType(elt, PsiComment.class, false);
  }

  public void commentRange(int startOffset, int endOffset, String commentPrefix, String commentSuffix, Commenter commenter) {
    if (breaksExistingComment(startOffset, true) || breaksExistingComment(endOffset, false)) {
      myWarning = CodeInsightBundle.message("block.comment.intersects.existing.comment");
      return;
    }
    final CharSequence chars = myDocument.getCharsSequence();
    LogicalPosition caretPosition = myCaret.getLogicalPosition();

    if (startOffset == 0 || chars.charAt(startOffset - 1) == '\n') {
      if (endOffset == myDocument.getTextLength() || endOffset > 0 && chars.charAt(endOffset - 1) == '\n') {
        CommonCodeStyleSettings settings = CodeStyle.getLanguageSettings(myFile);
        String space;
        if (!settings.BLOCK_COMMENT_AT_FIRST_COLUMN) {
          int line1 = myEditor.offsetToLogicalPosition(startOffset).line;
          int line2 = myEditor.offsetToLogicalPosition(endOffset - 1).line;
          IndentData minIndent = CommentUtil.getMinLineIndent(myDocument, line1, line2, myFile);
          if (minIndent == null) {
            minIndent = new IndentData(0);
          }
          space = minIndent.createIndentInfo().generateNewWhiteSpace(CodeStyle.getIndentOptions(myFile));
        }
        else {
          space = "";
        }
        final StringBuilder nestingPrefix = new StringBuilder(space).append(commentPrefix);
        if (!commentPrefix.endsWith("\n")) {
          nestingPrefix.append("\n");
        }
        String nestingSuffix = space + (commentSuffix.startsWith("\n") ? commentSuffix.substring(1) : commentSuffix) + "\n";
        TextRange range =
          insertNestedComments(startOffset, endOffset, nestingPrefix.toString(), nestingSuffix, commenter);
        if (range != null) {
          myCaret.setSelection(range.getStartOffset(), range.getEndOffset());
          LogicalPosition pos = new LogicalPosition(caretPosition.line + 1, caretPosition.column);
          myCaret.moveToLogicalPosition(pos);
        }
        return;
      }
    }

    TextRange range = insertNestedComments(startOffset, endOffset, commentPrefix, commentSuffix, commenter);
    if (range != null) {
      myCaret.setSelection(range.getStartOffset(), range.getEndOffset());
      LogicalPosition pos = new LogicalPosition(caretPosition.line, caretPosition.column + commentPrefix.length());
      myCaret.moveToLogicalPosition(pos);
    }
  }

  private boolean breaksExistingComment(int offset, boolean includingAfterLineComment) {
    if (!(myCommenter instanceof CodeDocumentationAwareCommenter) || !(myEditor instanceof EditorEx) || offset == 0) return false;
    CodeDocumentationAwareCommenter commenter = (CodeDocumentationAwareCommenter)myCommenter;
    HighlighterIterator it = ((EditorEx)myEditor).getHighlighter().createIterator(offset - 1);
    IElementType tokenType = it.getTokenType();
    return  (tokenType != null && (it.getEnd() > offset && (tokenType == commenter.getLineCommentTokenType() || 
                                                            tokenType == commenter.getBlockCommentTokenType() ||
                                                            tokenType == commenter.getDocumentationCommentTokenType()) || 
                                   includingAfterLineComment && it.getEnd() == offset && tokenType == commenter.getLineCommentTokenType() && 
                                   !(commenter instanceof CommenterWithLineSuffix)));
  }

  private boolean canDetectBlockComments() {
    return myEditor instanceof EditorEx && myCommenter instanceof CodeDocumentationAwareCommenter && 
           ((CodeDocumentationAwareCommenter)myCommenter).getBlockCommentTokenType() != null;
  }

  // should be called only if 'canDetectBlockComments' returns 'true'
  private TextRange getBlockCommentAt(int offset) {
    CodeDocumentationAwareCommenter commenter = (CodeDocumentationAwareCommenter)myCommenter;
    HighlighterIterator it = ((EditorEx)myEditor).getHighlighter().createIterator(offset);
    if (it.getTokenType() == commenter.getBlockCommentTokenType()) {
      return new TextRange(it.getStart(), it.getEnd());
    }
    if (docCommentIsBlockComment(commenter)) {
      PsiComment comment = getCommentAtOffset(offset);
      if (comment != null && commenter.isDocumentationComment(comment)) {
        return comment.getTextRange();
      }
    }
    return null;
  }

  private static boolean docCommentIsBlockComment(CodeDocumentationAwareCommenter commenter) {
    return commenter.getBlockCommentPrefix() != null && commenter.getDocumentationCommentPrefix() != null &&
           commenter.getDocumentationCommentPrefix().startsWith(commenter.getBlockCommentPrefix()) &&
           commenter.getBlockCommentSuffix() != null && commenter.getDocumentationCommentSuffix() != null &&
           commenter.getDocumentationCommentSuffix().endsWith(commenter.getBlockCommentSuffix());
  }

  private int doBoundCommentingAndGetShift(int offset,
                                           String commented,
                                           int skipLength,
                                           String toInsert,
                                           boolean skipBrace,
                                           TextRange selection) {
    if (commented == null && (offset == selection.getStartOffset() || offset + (skipBrace ? skipLength : 0) == selection.getEndOffset())) {
      return 0;
    }
    if (commented == null) {
      myDocument.insertString(offset + (skipBrace ? skipLength : 0), toInsert);
      return toInsert.length();
    }
    else {
      myDocument.replaceString(offset, offset + skipLength, commented);
      return commented.length() - skipLength;
    }
  }

  private TextRange insertNestedComments(int startOffset,
                                         int endOffset,
                                         String commentPrefix,
                                         String commentSuffix,
                                         Commenter commenter) {
    if (commenter instanceof SelfManagingCommenter) {
      final SelfManagingCommenter selfManagingCommenter = (SelfManagingCommenter)commenter;
      return selfManagingCommenter.insertBlockComment(
        startOffset,
        endOffset,
        myDocument,
        mySelfManagedCommenterData
      );
    }

    String normalizedPrefix = commentPrefix.trim();
    String normalizedSuffix = commentSuffix.trim();
    IntArrayList nestedCommentPrefixes = new IntArrayList();
    IntArrayList nestedCommentSuffixes = new IntArrayList();
    String commentedPrefix = commenter.getCommentedBlockCommentPrefix();
    String commentedSuffix = commenter.getCommentedBlockCommentSuffix();
    CharSequence chars = myDocument.getCharsSequence();
    boolean canDetectBlockComments = canDetectBlockComments();
    boolean warnAboutNestedComments = false;
    for (int i = startOffset; i < endOffset; ++i) {
      if (CharArrayUtil.regionMatches(chars, i, normalizedPrefix)) {
        if (commentedPrefix == null && canDetectBlockComments) {
          TextRange commentRange = getBlockCommentAt(i);
          // skipping prefixes outside of comments (e.g. in string literals) and inside comments
          if (commentRange == null || commentRange.getStartOffset() != i) continue;
          else warnAboutNestedComments = true;
        }
        nestedCommentPrefixes.add(i);
      }
      else if (CharArrayUtil.regionMatches(chars, i, normalizedSuffix)) {
        if (commentedSuffix == null && canDetectBlockComments) {
          TextRange commentRange = getBlockCommentAt(i);
          if (commentRange == null) {
            myWarning = CodeInsightBundle.message("block.comment.wrapping.suffix");
            myWarningLocation = myDocument.createRangeMarker(i, i);
            return null;
          }
        }
        nestedCommentSuffixes.add(i);
      }
    }
    if (warnAboutNestedComments) {
      myWarning = CodeInsightBundle.message("block.comment.nested.comment", nestedCommentPrefixes.size());
      myWarningLocation = myDocument.createRangeMarker(nestedCommentPrefixes.get(0), 
                                                       nestedCommentPrefixes.get(0) + normalizedPrefix.length());
    }
    int shift = 0;
    if (!(commentedSuffix == null &&
          !nestedCommentSuffixes.isEmpty() &&
          nestedCommentSuffixes.get(nestedCommentSuffixes.size() - 1) + commentSuffix.length() == endOffset)) {
      myDocument.insertString(endOffset, commentSuffix);
      shift += commentSuffix.length();
    }

    // process nested comments in back order
    int i = nestedCommentPrefixes.size() - 1;
    int j = nestedCommentSuffixes.size() - 1;
    final TextRange selection = new TextRange(startOffset, endOffset);
    while (i >= 0 && j >= 0) {
      final int prefixIndex = nestedCommentPrefixes.get(i);
      final int suffixIndex = nestedCommentSuffixes.get(j);
      if (prefixIndex > suffixIndex) {
        shift += doBoundCommentingAndGetShift(prefixIndex, commentedPrefix, normalizedPrefix.length(), commentSuffix, false, selection);
        --i;
      }
      else {
        //if (insertPos < myDocument.getTextLength() && Character.isWhitespace(myDocument.getCharsSequence().charAt(insertPos))) {
        //  insertPos = suffixIndex + commentSuffix.length();
        //}
        shift += doBoundCommentingAndGetShift(suffixIndex, commentedSuffix, normalizedSuffix.length(), commentPrefix, true, selection);
        --j;
      }
    }
    while (i >= 0) {
      final int prefixIndex = nestedCommentPrefixes.get(i);
      shift += doBoundCommentingAndGetShift(prefixIndex, commentedPrefix, normalizedPrefix.length(), commentSuffix, false, selection);
      --i;
    }
    while (j >= 0) {
      final int suffixIndex = nestedCommentSuffixes.get(j);
      shift += doBoundCommentingAndGetShift(suffixIndex, commentedSuffix, normalizedSuffix.length(), commentPrefix, true, selection);
      --j;
    }
    if (!(commentedPrefix == null && !nestedCommentPrefixes.isEmpty() && nestedCommentPrefixes.get(0) == startOffset)) {
      myDocument.insertString(startOffset, commentPrefix);
      shift += commentPrefix.length();
    }

    RangeMarker marker = myDocument.createRangeMarker(startOffset, endOffset + shift);
    try {
      return processDocument(myDocument, marker, commenter, true);
    }
    finally {
      marker.dispose();
    }
  }

  static TextRange processDocument(Document document, RangeMarker marker, Commenter commenter, boolean escape) {
    if (commenter instanceof EscapingCommenter) {
      if (escape) {
        ((EscapingCommenter)commenter).escape(document, marker);
      }
      else {
        ((EscapingCommenter)commenter).unescape(document, marker);
      }
    }
    return TextRange.create(marker.getStartOffset(), marker.getEndOffset());
  }

  private static int getNearest(String text, String pattern, int position) {
    int result = text.indexOf(pattern, position);
    return result == -1 ? text.length() : result;
  }

  static void commentNestedComments(@NotNull Document document, TextRange range, Commenter commenter) {
    final int offset = range.getStartOffset();
    final IntArrayList toReplaceWithComments = new IntArrayList();
    final IntArrayList prefixes = new IntArrayList();

    final String text = document.getCharsSequence().subSequence(range.getStartOffset(), range.getEndOffset()).toString();
    final String commentedPrefix = commenter.getCommentedBlockCommentPrefix();
    final String commentedSuffix = commenter.getCommentedBlockCommentSuffix();
    final String commentPrefix = commenter.getBlockCommentPrefix();
    final String commentSuffix = commenter.getBlockCommentSuffix();


    int nearestSuffix = getNearest(text, commentedSuffix, 0);
    int nearestPrefix = getNearest(text, commentedPrefix, 0);
    int level = 0;
    int lastSuffix = -1;
    for (int i = Math.min(nearestPrefix, nearestSuffix); i < text.length(); i = Math.min(nearestPrefix, nearestSuffix)) {
      if (i > nearestPrefix) {
        nearestPrefix = getNearest(text, commentedPrefix, i);
        continue;
      }
      if (i > nearestSuffix) {
        nearestSuffix = getNearest(text, commentedSuffix, i);
        continue;
      }
      if (i == nearestPrefix) {
        if (level <= 0) {
          if (lastSuffix != -1) {
            toReplaceWithComments.add(lastSuffix);
          }
          level = 1;
          lastSuffix = -1;
          toReplaceWithComments.add(i);
          prefixes.add(i);
        }
        else {
          level++;
        }
        nearestPrefix = getNearest(text, commentedPrefix, nearestPrefix + 1);
      }
      else {
        lastSuffix = i;
        level--;
        nearestSuffix = getNearest(text, commentedSuffix, nearestSuffix + 1);
      }
    }
    if (lastSuffix != -1) {
      toReplaceWithComments.add(lastSuffix);
    }

    int prefixIndex = prefixes.size() - 1;
    for (int i = toReplaceWithComments.size() - 1; i >= 0; i--) {
      int position = toReplaceWithComments.get(i);
      if (prefixIndex >= 0 && position == prefixes.get(prefixIndex)) {
        prefixIndex--;
        document.replaceString(offset + position, offset + position + commentedPrefix.length(), commentPrefix);
      }
      else {
        document.replaceString(offset + position, offset + position + commentedSuffix.length(), commentSuffix);
      }
    }
  }

  private TextRange expandRange(int delOffset1, int delOffset2) {
    CharSequence chars = myDocument.getCharsSequence();
    int offset1 = CharArrayUtil.shiftBackward(chars, delOffset1 - 1, " \t");
    if (offset1 < 0 || chars.charAt(offset1) == '\n' || chars.charAt(offset1) == '\r') {
      int offset2 = CharArrayUtil.shiftForward(chars, delOffset2, " \t");
      if (offset2 == myDocument.getTextLength() || chars.charAt(offset2) == '\r' || chars.charAt(offset2) == '\n') {
        delOffset1 = offset1 + 1;
        if (offset2 < myDocument.getTextLength()) {
          delOffset2 = offset2 + 1;
          if (chars.charAt(offset2) == '\r' && offset2 + 1 < myDocument.getTextLength() && chars.charAt(offset2 + 1) == '\n') {
            delOffset2++;
          }
        }
      }
    }
    return new TextRange(delOffset1, delOffset2);
  }

  private Couple<TextRange> findCommentBlock(TextRange range, String commentPrefix, String commentSuffix) {
    CharSequence chars = myDocument.getCharsSequence();
    int startOffset = range.getStartOffset();
    boolean endsProperly = CharArrayUtil.regionMatches(chars, range.getEndOffset() - commentSuffix.length(), commentSuffix);

    TextRange start = expandRange(startOffset, startOffset + commentPrefix.length());
    TextRange end;
    if (endsProperly) {
      end = expandRange(range.getEndOffset() - commentSuffix.length(), range.getEndOffset());
    }
    else {
      end = new TextRange(range.getEndOffset(), range.getEndOffset());
    }

    return Couple.of(start, end);
  }

  public void uncommentRange(TextRange range, String commentPrefix, String commentSuffix, Commenter commenter) {
    if (commenter instanceof SelfManagingCommenter) {
      final SelfManagingCommenter selfManagingCommenter = (SelfManagingCommenter)commenter;
      selfManagingCommenter.uncommentBlockComment(
        range.getStartOffset(),
        range.getEndOffset(),
        myDocument,
        mySelfManagedCommenterData
      );
      return;
    }

    String text = myDocument.getCharsSequence().subSequence(range.getStartOffset(), range.getEndOffset()).toString();
    int startOffset = range.getStartOffset();
    //boolean endsProperly = CharArrayUtil.regionMatches(chars, range.getEndOffset() - commentSuffix.length(), commentSuffix);
    List<Couple<TextRange>> ranges = new ArrayList<>();


    if (commenter instanceof CustomUncommenter) {
      /*
        In case of custom uncommenter, we need to ask it for list of [commentOpen-start,commentOpen-end], [commentClose-start,commentClose-end]
        and shift if according to current offset
       */
      CustomUncommenter customUncommenter = (CustomUncommenter)commenter;
      for (Couple<TextRange> coupleFromCommenter : customUncommenter.getCommentRangesToDelete(text)) {
        TextRange openComment = coupleFromCommenter.first.shiftRight(startOffset);
        TextRange closeComment = coupleFromCommenter.second.shiftRight(startOffset);
        ranges.add(Couple.of(openComment, closeComment));
      }
    }
    else {
      // If commenter is not custom, we need to get this list by our selves
      int position = 0;
      while (true) {
        int start = getNearest(text, commentPrefix, position);
        if (start == text.length()) {
          break;
        }
        position = start;
        int end = getNearest(text, commentSuffix, position + commentPrefix.length()) + commentSuffix.length();
        position = end;
        Couple<TextRange> pair =
          findCommentBlock(new TextRange(start + startOffset, end + startOffset), commentPrefix, commentSuffix);
        ranges.add(pair);
      }
    }

    RangeMarker marker = myDocument.createRangeMarker(range);
    try {
      for (int i = ranges.size() - 1; i >= 0; i--) {
        Couple<TextRange> toDelete = ranges.get(i);
        myDocument.deleteString(toDelete.first.getStartOffset(), toDelete.first.getEndOffset());
        int shift = toDelete.first.getEndOffset() - toDelete.first.getStartOffset();
        myDocument.deleteString(toDelete.second.getStartOffset() - shift, toDelete.second.getEndOffset() - shift);
        if (commenter.getCommentedBlockCommentPrefix() != null) {
          commentNestedComments(myDocument, new TextRange(toDelete.first.getEndOffset() - shift, toDelete.second.getStartOffset() - shift),
                                commenter);
        }
      }

      processDocument(myDocument, marker, commenter, false);
    }
    finally {
      marker.dispose();
    }
  }
}

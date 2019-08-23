// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions;

import com.intellij.ide.DataManager;
import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.lang.Commenter;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.codeInsight.editorActions.JoinLinesHandlerDelegate.CANNOT_JOIN;

public class JoinLinesHandler extends EditorActionHandler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.editorActions.JoinLinesHandler");
  private final EditorActionHandler myOriginalHandler;

  public JoinLinesHandler(EditorActionHandler originalHandler) {
    super(true);
    myOriginalHandler = originalHandler;
  }

  @NotNull
  private static TextRange findStartAndEnd(@NotNull CharSequence text, int start, int end, int maxoffset) {
    while (start > 0 && isSpaceOrTab(text, start)) start--;
    while (end < maxoffset && isSpaceOrTab(text, end)) end++;
    return new TextRange(start, end);
  }

  private static boolean isSpaceOrTab(@NotNull CharSequence text, int index) {
    char c = text.charAt(index);
    return c == ' ' || c == '\t';
  }

  @Override
  public void doExecute(@NotNull final Editor editor, @Nullable Caret caret, final DataContext dataContext) {
    assert caret != null;

    if (editor.isViewer() || !EditorModificationUtil.requestWriting(editor)) return;

    if (!(editor.getDocument() instanceof DocumentEx)) {
      myOriginalHandler.execute(editor, caret, dataContext);
      return;
    }
    final DocumentEx doc = (DocumentEx)editor.getDocument();
    final Project project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(editor.getContentComponent()));
    if (project == null) return;

    final PsiDocumentManager docManager = PsiDocumentManager.getInstance(project);
    PsiFile psiFile = docManager.getPsiFile(doc);

    if (psiFile == null) {
      myOriginalHandler.execute(editor, caret, dataContext);
      return;
    }

    LogicalPosition caretPosition = caret.getLogicalPosition();
    int startLine = caretPosition.line;
    int endLine = startLine + 1;
    if (caret.hasSelection()) {
      startLine = doc.getLineNumber(caret.getSelectionStart());
      endLine = doc.getLineNumber(caret.getSelectionEnd());
      if (doc.getLineStartOffset(endLine) == caret.getSelectionEnd()) endLine--;
    }

    final int startReformatOffset = CharArrayUtil.shiftBackward(doc.getCharsSequence(), doc.getLineEndOffset(startLine), " \t");
    // joining lines, several times if selection is multiline
    int lineCount = endLine - startLine;
    int line = startLine;

    ((ApplicationImpl)ApplicationManager.getApplication()).runWriteActionWithCancellableProgressInDispatchThread(
      "Join Lines", project, null, indicator -> {
        indicator.setIndeterminate(false);
        Ref<Integer> caretRestoreOffset = new Ref<>(-1);
        CodeEditUtil.setNodeReformatStrategy(node -> node.getTextRange().getStartOffset() >= startReformatOffset);
        try {
          int count = 0;
          while (count < lineCount) {
            indicator.checkCanceled();
            indicator.setFraction(((double)count) / lineCount);
            int beforeLines = doc.getLineCount();
            ProgressManager.getInstance().executeNonCancelableSection(
              () -> doJoinTwoLines(doc, project, docManager, psiFile, line, caretRestoreOffset));
            int afterLines = doc.getLineCount();
            // Single Join two lines procedure could join more than two (e.g. if it removes braces)
            count += Math.max(beforeLines - afterLines, 1);
          }
        }
        finally {
          CodeEditUtil.setNodeReformatStrategy(null);
        }

        positionCaret(editor, caret, caretRestoreOffset.get());
      });
  }

  private static void positionCaret(Editor editor, Caret caret, int caretRestoreOffset) {
    if (caret.hasSelection()) {
      caret.moveToOffset(caret.getSelectionEnd());
    }
    else if (caretRestoreOffset != CANNOT_JOIN) {
      caret.moveToOffset(caretRestoreOffset);
      if (caret == editor.getCaretModel().getPrimaryCaret()) { // performance
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
      }
      caret.removeSelection();
    }
  }

  private static void doJoinTwoLines(@NotNull DocumentEx doc,
                                     @NotNull Project project,
                                     @NotNull PsiDocumentManager docManager,
                                     @NotNull PsiFile psiFile,
                                     int startLine,
                                     Ref<Integer> caretRestoreOffset) {
    if (startLine >= doc.getLineCount() - 1) return;

    docManager.doPostponedOperationsAndUnblockDocument(doc);
    docManager.commitDocument(doc);
    CharSequence text = doc.getCharsSequence();
    JoinLinesOffsets offsets = calcJoinLinesOffsets(psiFile, doc, startLine);

    if (offsets.isStartLineEndsWithComment() && !offsets.isNextLineStartsWithComment()) {
      tryConvertEndOfLineComment(doc, offsets.elementAtStartLineEnd);
      offsets = calcJoinLinesOffsets(psiFile, doc, startLine);
    }

    TextRange limits = findStartAndEnd(text, offsets.lastNonSpaceOffsetInStartLine, offsets.firstNonSpaceOffsetInNextLine, doc.getTextLength());
    int start = limits.getStartOffset();
    int end = limits.getEndOffset();
    // run raw joiners
    int rc = -1;
    for (JoinLinesHandlerDelegate delegate: JoinLinesHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate instanceof JoinRawLinesHandlerDelegate) {
        rc = ((JoinRawLinesHandlerDelegate)delegate).tryJoinRawLines(doc, psiFile, start, end);
        if (rc != CANNOT_JOIN) {
          caretRestoreOffset.set(checkOffset(rc, delegate, doc));
          break;
        }
      }
    }
    if (rc == CANNOT_JOIN) { // remove indents and newline, run non-raw joiners
      if (offsets.lastNonSpaceOffsetInStartLine == doc.getLineStartOffset(startLine)) {
        doc.deleteString(doc.getLineStartOffset(startLine), offsets.firstNonSpaceOffsetInNextLine);

        int indent = -1;
        try {
          docManager.commitDocument(doc);
          indent = CodeStyleManager.getInstance(project).adjustLineIndent(psiFile, startLine == 0 ? 0 : doc.getLineStartOffset(startLine));
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }

        if (caretRestoreOffset.get() == CANNOT_JOIN) {
          caretRestoreOffset.set(indent);
        }

        return;
      }

      doc.deleteString(offsets.lineEndOffset, offsets.lineEndOffset + doc.getLineSeparatorLength(startLine));

      text = doc.getCharsSequence();
      limits = findStartAndEnd(text, offsets.lineEndOffset - 1, offsets.lineEndOffset, doc.getTextLength());
      start = limits.getStartOffset(); end = limits.getEndOffset();

      // Check if we're joining splitted string literal.
      docManager.commitDocument(doc);

      for(JoinLinesHandlerDelegate delegate: JoinLinesHandlerDelegate.EP_NAME.getExtensionList()) {
        rc = checkOffset(delegate.tryJoinLines(doc, psiFile, start, end), delegate, doc);
        if (rc != CANNOT_JOIN) break;
      }
    }

    if (rc != CANNOT_JOIN) {
      RangeMarker marker = doc.createRangeMarker(rc, rc);
      docManager.doPostponedOperationsAndUnblockDocument(doc);
      if (caretRestoreOffset.get() == CANNOT_JOIN && marker.isValid()) {
        caretRestoreOffset.set(marker.getStartOffset());
      }
      return;
    }
    docManager.doPostponedOperationsAndUnblockDocument(doc);

    int replaceStart = start == offsets.lineEndOffset ? start : start + 1;
    if (caretRestoreOffset.get() == CANNOT_JOIN) caretRestoreOffset.set(replaceStart);


    if (offsets.isStartLineEndsWithComment() && offsets.isNextLineStartsWithComment()) {
      boolean adjacentLineComments = false;
      if (text.charAt(end) == '*' && end < text.length() && text.charAt(end + 1) != '/') {
        end++;
        while (end < doc.getTextLength() && isSpaceOrTab(text, end)) end++;
      }
      else if (!offsets.isJoiningSameComment() &&
               !(replaceStart >= 2 && text.charAt(replaceStart - 2) == '*' && text.charAt(replaceStart - 1) == '/') &&
               text.charAt(end) == '/' && end + 1 < text.length() && text.charAt(end + 1) == '/') {
        adjacentLineComments = true;
        end += 2;
        while (end < doc.getTextLength() && isSpaceOrTab(text, end)) end++;
      }

      doc.replaceString(replaceStart, end, adjacentLineComments || offsets.isJoiningSameComment() ? " " : "");
      return;
    }

    while (end < doc.getTextLength() && isSpaceOrTab(text, end)) end++;

    int spacesToCreate = CodeStyleManager.getInstance(project).getSpacing(psiFile, end);
    if (spacesToCreate < 0) spacesToCreate = 1;
    String spacing = StringUtil.repeatSymbol(' ', spacesToCreate);

    doc.replaceString(replaceStart, end, spacing);
    docManager.commitDocument(doc);

    if (start <= doc.getLineStartOffset(startLine)) {
      try {
        CodeStyleManager.getInstance(project).adjustLineIndent(psiFile, doc.getLineStartOffset(startLine));
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
      docManager.commitDocument(doc);
    }
  }

  private static int checkOffset(int offset, JoinLinesHandlerDelegate delegate, DocumentEx doc) {
    if (offset == CANNOT_JOIN) return offset;
    if (offset < 0) {
      LOG.error("Handler returned negative offset: handler class="+delegate.getClass()+"; offset="+offset);
      return 0;
    } else if (offset > doc.getTextLength()) {
      LOG.error("Handler returned an offset which exceeds the document length: handler class=" + delegate.getClass() + 
                "; offset=" + offset + "; length=" + doc.getTextLength());
      return doc.getTextLength();
    }
    return offset;
  }

  private static class JoinLinesOffsets {
    int lineEndOffset;
    int lastNonSpaceOffsetInStartLine;
    int firstNonSpaceOffsetInNextLine;
    PsiComment commentAtLineEnd;
    PsiComment commentAtLineStart;
    boolean isStartLineEndsWithComment() { return commentAtLineEnd != null; }
    boolean isNextLineStartsWithComment() { return commentAtLineStart != null; }
    boolean isJoiningSameComment() { return commentAtLineStart == commentAtLineEnd; }
    PsiElement elementAtStartLineEnd;
  }

  private static JoinLinesOffsets calcJoinLinesOffsets(PsiFile psiFile, Document doc, int startLine) {
    JoinLinesOffsets offsets = new JoinLinesOffsets();
    CharSequence text = doc.getCharsSequence();
    offsets.lineEndOffset = doc.getLineEndOffset(startLine);
    offsets.firstNonSpaceOffsetInNextLine = doc.getLineStartOffset(startLine + 1);
    while (offsets.firstNonSpaceOffsetInNextLine < text.length() - 1
           && (text.charAt(offsets.firstNonSpaceOffsetInNextLine) == ' ' || text.charAt(offsets.firstNonSpaceOffsetInNextLine) == '\t'))
    {
      offsets.firstNonSpaceOffsetInNextLine++;
    }
    PsiElement elementAtNextLineStart = psiFile.findElementAt(offsets.firstNonSpaceOffsetInNextLine);
    offsets.commentAtLineStart = getCommentElement(elementAtNextLineStart);

    offsets.lastNonSpaceOffsetInStartLine = offsets.lineEndOffset;
    while (offsets.lastNonSpaceOffsetInStartLine > 0 && isSpaceOrTab(text, offsets.lastNonSpaceOffsetInStartLine - 1)) {
      offsets.lastNonSpaceOffsetInStartLine--;
    }
    int elemOffset = offsets.lastNonSpaceOffsetInStartLine > doc.getLineStartOffset(startLine) ? offsets.lastNonSpaceOffsetInStartLine - 1 : -1;
    offsets.elementAtStartLineEnd = elemOffset == -1 ? null : psiFile.findElementAt(elemOffset);
    offsets.commentAtLineEnd = getCommentElement(offsets.elementAtStartLineEnd);
    return offsets;

  }

  private static void tryConvertEndOfLineComment(Document doc, PsiElement commentElement) {
    Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(commentElement.getLanguage());
    if (commenter instanceof CodeDocumentationAwareCommenter) {
      CodeDocumentationAwareCommenter docCommenter = (CodeDocumentationAwareCommenter) commenter;
      String lineCommentPrefix = commenter.getLineCommentPrefix();
      String blockCommentPrefix = commenter.getBlockCommentPrefix();
      String blockCommentSuffix = commenter.getBlockCommentSuffix();
      if (commentElement.getNode().getElementType() == docCommenter.getLineCommentTokenType() &&
        blockCommentPrefix != null && blockCommentSuffix != null && lineCommentPrefix != null) {
        String commentText = StringUtil.trimStart(commentElement.getText(), lineCommentPrefix);
        String suffix = docCommenter.getBlockCommentSuffix();
        if (suffix != null && suffix.length() > 1) {
          String fixedSuffix = suffix.charAt(0)+" "+suffix.substring(1);
          commentText = commentText.replace(suffix, fixedSuffix);
        }
        try {
          Project project = commentElement.getProject();
          PsiParserFacade parserFacade = PsiParserFacade.SERVICE.getInstance(project);
          PsiComment newComment = parserFacade.createBlockCommentFromText(commentElement.getLanguage(), commentText);
          commentElement.replace(newComment);
          PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(doc);
        }
        catch (IncorrectOperationException e) {
          LOG.info("Failed to replace line comment with block comment", e);
        }
      }
    }
  }

  private static PsiComment getCommentElement(@Nullable final PsiElement element) {
    return PsiTreeUtil.getParentOfType(element, PsiComment.class, false);
  }
}

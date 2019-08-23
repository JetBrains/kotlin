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
import com.intellij.openapi.progress.ProgressIndicator;
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

import java.util.ArrayList;
import java.util.List;

import static com.intellij.codeInsight.editorActions.JoinLinesHandlerDelegate.CANNOT_JOIN;

public class JoinLinesHandler extends EditorActionHandler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.editorActions.JoinLinesHandler");
  private final EditorActionHandler myOriginalHandler;

  public JoinLinesHandler(EditorActionHandler originalHandler) {
    super(true);
    myOriginalHandler = originalHandler;
  }

  @NotNull
  private static TextRange findStartAndEnd(@NotNull CharSequence text, int start, int end) {
    while (start > 0 && isSpaceOrTab(text, start)) start--;
    end = StringUtil.skipWhitespaceForward(text, end);
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
          convertEndComments(psiFile, doc, line, lineCount);
          ProgressManager.checkCanceled();
          int newEndLine = processRawJoiners(doc, docManager, psiFile, line, lineCount, caretRestoreOffset, indicator);
          int newCount = newEndLine - line;
          int count = 0;
          while (count < newCount) {
            indicator.checkCanceled();
            indicator.setFraction(((double)count) / newCount * 0.7 + 0.3);
            int beforeLines = doc.getLineCount();
            ProgressManager.getInstance().executeNonCancelableSection(
              () -> CodeStyleManager.getInstance(project).performActionWithFormatterDisabled(
                (Runnable)(() -> doJoinTwoLines(doc, project, docManager, psiFile, line, caretRestoreOffset))));
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

  private static int processRawJoiners(@NotNull DocumentEx doc,
                                       @NotNull PsiDocumentManager docManager,
                                       @NotNull PsiFile psiFile,
                                       int startLine,
                                       int lineCount,
                                       Ref<Integer> caretRestoreOffset,
                                       ProgressIndicator indicator) {
    int count = 0;
    List<JoinLinesHandlerDelegate> list = JoinLinesHandlerDelegate.EP_NAME.getExtensionList();
    int beforeLines = doc.getLineCount();
    CharSequence text = doc.getCharsSequence();
    while (count < lineCount) {
      indicator.checkCanceled();
      indicator.setFraction(((double)count) / lineCount * 0.3);
      JoinLinesOffsets offsets = new JoinLinesOffsets(doc, startLine);

      TextRange limits = findStartAndEnd(text, offsets.lastNonSpaceOffsetInStartLine, offsets.firstNonSpaceOffsetInNextLine);
      int start = limits.getStartOffset();
      int end = limits.getEndOffset();
      int rc = CANNOT_JOIN;
      for (JoinLinesHandlerDelegate delegate : list) {
        if (delegate instanceof JoinRawLinesHandlerDelegate) {
          rc = ((JoinRawLinesHandlerDelegate)delegate).tryJoinRawLines(doc, psiFile, start, end);
          if (rc != CANNOT_JOIN) {
            caretRestoreOffset.set(checkOffset(rc, delegate, doc));
            break;
          }
        }
      }
      if (rc == CANNOT_JOIN) {
        startLine++;
        count++;
      }
      else {
        docManager.doPostponedOperationsAndUnblockDocument(doc);
        docManager.commitDocument(doc);
        int afterLines = doc.getLineCount();
        // Single Join two lines procedure could join more than two (e.g. if it removes braces)
        count += Math.max(beforeLines - afterLines, 1);
        beforeLines = afterLines;
        text = doc.getCharsSequence();
      }
    }
    return startLine;
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
    JoinLinesOffsets offsets = new JoinLinesOffsets(doc, startLine);

    // remove indents and newline, run non-raw joiners
    if (offsets.lastNonSpaceOffsetInStartLine == doc.getLineStartOffset(startLine)) {
      doc.deleteString(doc.getLineStartOffset(startLine), offsets.firstNonSpaceOffsetInNextLine);

      docManager.commitDocument(doc);
      int indent =
        CodeStyleManager.getInstance(project).adjustLineIndent(psiFile, startLine == 0 ? 0 : doc.getLineStartOffset(startLine));

      if (caretRestoreOffset.get() == CANNOT_JOIN) {
        caretRestoreOffset.set(indent);
      }

      return;
    }

    doc.deleteString(offsets.lineEndOffset, offsets.lineEndOffset + doc.getLineSeparatorLength(startLine));

    CharSequence text = doc.getCharsSequence();
    TextRange limits = findStartAndEnd(text, offsets.lineEndOffset - 1, offsets.lineEndOffset);
    int start = limits.getStartOffset();
    int end = limits.getEndOffset();

    docManager.commitDocument(doc);

    int rc = CANNOT_JOIN;
    for (JoinLinesHandlerDelegate delegate : JoinLinesHandlerDelegate.EP_NAME.getExtensionList()) {
      rc = checkOffset(delegate.tryJoinLines(doc, psiFile, start, end), delegate, doc);
      if (rc != CANNOT_JOIN) break;
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

    postProcessWhitespace(doc, project, docManager, psiFile, text, limits, replaceStart, doc.getLineStartOffset(startLine));
    docManager.commitDocument(doc);
  }

  private static void postProcessWhitespace(@NotNull DocumentEx doc,
                                            @NotNull Project project,
                                            @NotNull PsiDocumentManager docManager,
                                            @NotNull PsiFile psiFile,
                                            CharSequence text,
                                            TextRange limits, int replaceStart, int lineStart) {
    int end = StringUtil.skipWhitespaceForward(text, limits.getEndOffset());

    int spacesToCreate = CodeStyleManager.getInstance(project).getSpacing(psiFile, end);
    if (spacesToCreate < 0) spacesToCreate = 1;
    String spacing = StringUtil.repeatSymbol(' ', spacesToCreate);

    doc.replaceString(replaceStart, end, spacing);

    if (limits.getStartOffset() <= lineStart) {
      docManager.commitDocument(doc);
      CodeStyleManager.getInstance(project).adjustLineIndent(psiFile, lineStart);
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
    final int lineEndOffset;
    final int lastNonSpaceOffsetInStartLine;
    final int firstNonSpaceOffsetInNextLine;

    JoinLinesOffsets(Document doc, int startLine) {
      CharSequence text = doc.getCharsSequence();
      this.lineEndOffset = doc.getLineEndOffset(startLine);
      this.firstNonSpaceOffsetInNextLine = StringUtil.skipWhitespaceForward(text, doc.getLineStartOffset(startLine + 1));
      this.lastNonSpaceOffsetInStartLine = StringUtil.skipWhitespaceBackward(text, this.lineEndOffset);
    }
  }

  private static void convertEndComments(PsiFile psiFile, Document doc, int startLine, int lineCount) {
    List<PsiComment> endComments = new ArrayList<>();
    CharSequence text = doc.getCharsSequence();
    for (int i = 0; i < lineCount; i++) {
      int line = startLine + i;
      int lineEnd = doc.getLineEndOffset(line);
      int lastNonSpaceOffset = StringUtil.skipWhitespaceBackward(text, lineEnd);
      if (lastNonSpaceOffset > doc.getLineStartOffset(line)) {
        PsiComment comment = getCommentElement(psiFile.findElementAt(lastNonSpaceOffset - 1));
        if (comment != null) {
          int nextStart = StringUtil.skipWhitespaceForward(text, doc.getLineStartOffset(line + 1));
          if (getCommentElement(psiFile.findElementAt(nextStart)) == null) {
            endComments.add(comment);
          }
        }
      }
    }
    boolean changed = false;
    for (PsiComment comment : endComments) {
      changed |= tryConvertEndOfLineComment(comment);
      ProgressManager.checkCanceled();
    }
    if (changed) {
      PsiDocumentManager.getInstance(psiFile.getProject()).doPostponedOperationsAndUnblockDocument(doc);
    }
  }

  private static boolean tryConvertEndOfLineComment(PsiElement commentElement) {
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
          return true;
        }
        catch (IncorrectOperationException e) {
          LOG.info("Failed to replace line comment with block comment", e);
        }
      }
    }
    return false;
  }

  private static PsiComment getCommentElement(@Nullable final PsiElement element) {
    return PsiTreeUtil.getParentOfType(element, PsiComment.class, false);
  }
}

// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.generation;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.CommentUtil;
import com.intellij.codeInsight.actions.MultiCaretCodeInsightActionHandler;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.formatting.IndentData;
import com.intellij.ide.highlighter.custom.SyntaxTable;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.AbstractFileType;
import com.intellij.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.impl.source.tree.injected.InjectedCaret;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.DocumentUtil;
import com.intellij.util.containers.IntArrayList;
import com.intellij.util.text.CharArrayUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CommentByLineCommentHandler extends MultiCaretCodeInsightActionHandler {

  private static final Key<Boolean> INJECTION_FORBIDS_LINE_COMMENTS = Key.create("INJECTION_FORBIDS_LINE_COMMENTS");

  /**
   * Disable line commenting in an injected file making this action operate on its host file instead.
   *
   * @param file injected file where line comment action shouldn't be available
   */
  public static void markInjectedFileUnsuitableForLineComment(@NotNull PsiFile file) {
    if (!InjectedLanguageManager.getInstance(file.getProject()).isInjectedFragment(file)) {
      throw new IllegalArgumentException("This method should be called only on injected files");
    }

    file.putUserData(INJECTION_FORBIDS_LINE_COMMENTS, true);
  }

  private final List<Block> myBlocks = new ArrayList<>();

  @Override
  // first pass - adjacent carets are grouped into blocks
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Caret caret, @NotNull PsiFile file) {
    file = file.getViewProvider().getPsi(file.getViewProvider().getBaseLanguage());

    PsiElement context = InjectedLanguageManager.getInstance(file.getProject()).getInjectionHost(file);

    if (context != null && shouldCommentInHostFile(file, context)) {
      file = context.getContainingFile();
      editor = editor instanceof EditorWindow ? ((EditorWindow)editor).getDelegate() : editor;
      caret = caret instanceof InjectedCaret ? ((InjectedCaret)caret).getDelegate() : caret;
    }

    Document document = editor.getDocument();
    boolean hasSelection = caret.hasSelection();
    int startOffset = caret.getSelectionStart();
    int endOffset = caret.getSelectionEnd();

    FoldRegion fold = editor.getFoldingModel().getCollapsedRegionAtOffset(startOffset);
    if (fold != null && fold.shouldNeverExpand() && fold.getStartOffset() == startOffset && fold.getEndOffset() == endOffset) {
      // Foldings that never expand are automatically selected, so the fact it is selected must not interfere with commenter's logic
      hasSelection = false;
    }

    if (document.getTextLength() == 0) return;

    while (true) {
      int firstLineStart = DocumentUtil.getLineStartOffset(startOffset, document);
      FoldRegion collapsedAt = editor.getFoldingModel().getCollapsedRegionAtOffset(firstLineStart - 1);
      if (collapsedAt == null) break;
      int regionStartOffset = collapsedAt.getStartOffset();
      if (regionStartOffset >= startOffset) break;
      startOffset = regionStartOffset;
    }
    if (!hasSelection || !DocumentUtil.isAtLineStart(endOffset, document)) {
      while (true) {
        int lastLineEnd = DocumentUtil.getLineEndOffset(endOffset, document);
        FoldRegion collapsedAt = editor.getFoldingModel().getCollapsedRegionAtOffset(lastLineEnd);
        if (collapsedAt == null) break;
        int regionEndOffset = collapsedAt.getEndOffset();
        if (regionEndOffset <= endOffset) break;
        endOffset = regionEndOffset;
      }
    }

    int startLine = document.getLineNumber(startOffset);
    int endLine = document.getLineNumber(endOffset);

    if (endLine > startLine && document.getLineStartOffset(endLine) == endOffset) {
      endLine--;
    }

    Block lastBlock = myBlocks.isEmpty() ? null : myBlocks.get(myBlocks.size() - 1);
    Block currentBlock;
    if (lastBlock == null || lastBlock.editor != editor || lastBlock.psiFile != file || startLine > (lastBlock.endLine + 1)) {
      currentBlock = new Block();
      currentBlock.editor = editor;
      currentBlock.psiFile = file;
      currentBlock.startLine = startLine;
      myBlocks.add(currentBlock);
    }
    else {
      currentBlock = lastBlock;
    }
    currentBlock.carets.add(caret);
    currentBlock.endLine = endLine;

    boolean wholeLinesSelected = !hasSelection ||
                                 startOffset == document.getLineStartOffset(document.getLineNumber(startOffset)) &&
                                 endOffset == document.getLineEndOffset(document.getLineNumber(endOffset - 1)) + 1;
    boolean startingNewLineComment = !hasSelection
                                     && DocumentUtil.isLineEmpty(document, document.getLineNumber(startOffset))
                                     && !Comparing.equal(IdeActions.ACTION_COMMENT_LINE,
                                                         ActionManagerEx.getInstanceEx().getPrevPreformedActionId());
    currentBlock.caretUpdate = startingNewLineComment ? CaretUpdate.PUT_AT_COMMENT_START :
                               !hasSelection ? CaretUpdate.SHIFT_DOWN :
                               wholeLinesSelected ? CaretUpdate.RESTORE_SELECTION : null;
    }

  private static boolean shouldCommentInHostFile(@NotNull PsiFile file, @NotNull PsiElement context) {
    if (file.getUserData(INJECTION_FORBIDS_LINE_COMMENTS) != null) {
      return true;
    }
    if (context.textContains('\'') || context.textContains('\"') || context.textContains('/')) {
      final String s = context.getText();
      return StringUtil.startsWith(s, "\"") || StringUtil.startsWith(s, "\'") || StringUtil.startsWith(s, "/");
    }
    return false;
  }

  @Override
  public void postInvoke() {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.comment.line");

    // second pass - determining whether we need to comment or to uncomment
    boolean allLinesCommented = true;
    for (Block block : myBlocks) {
      int startLine = block.startLine;
      int endLine = block.endLine;
      Document document = block.editor.getDocument();
      PsiFile psiFile = block.psiFile;
      block.startOffsets = new int[endLine - startLine + 1];
      block.endOffsets = new int[endLine - startLine + 1];
      block.commenters = new Commenter[endLine - startLine + 1];
      block.commenterStateMap = new THashMap<>();
      CharSequence chars = document.getCharsSequence();

      boolean singleline = startLine == endLine;
      int offset = document.getLineStartOffset(startLine);
      offset = CharArrayUtil.shiftForward(chars, offset, " \t");

      int endOffset = CharArrayUtil.shiftBackward(chars, document.getLineEndOffset(endLine), " \t\n");

      block.blockSuitableCommenter = getBlockSuitableCommenter(psiFile, offset, endOffset);
      Language lineStartLanguage = getLineStartLanguage(block.editor, psiFile, startLine);
      CommonCodeStyleSettings languageSettings = CodeStyle.getLanguageSettings(psiFile, lineStartLanguage);
      block.commentWithIndent = !languageSettings.LINE_COMMENT_AT_FIRST_COLUMN;
      block.addSpace = languageSettings.LINE_COMMENT_ADD_SPACE;

      for (int line = startLine; line <= endLine; line++) {
        Commenter commenter = block.blockSuitableCommenter != null ? block.blockSuitableCommenter : findCommenter(block.editor, psiFile, line);
        if (commenter == null || commenter.getLineCommentPrefix() == null
                                 && (commenter.getBlockCommentPrefix() == null || commenter.getBlockCommentSuffix() == null)) {
          block.skip = true;
          break;
        }

        if (commenter instanceof SelfManagingCommenter && block.commenterStateMap.get(commenter) == null) {
          final SelfManagingCommenter selfManagingCommenter = (SelfManagingCommenter)commenter;
          CommenterDataHolder state = selfManagingCommenter.createLineCommentingState(startLine, endLine, document, psiFile);
          if (state == null) state = SelfManagingCommenter.EMPTY_STATE;
          block.commenterStateMap.put(selfManagingCommenter, state);
        }

        block.commenters[line - startLine] = commenter;
        if (!isLineCommented(block, line, commenter) && (singleline || !DocumentUtil.isLineEmpty(document, line))) {
          allLinesCommented = false;
          if (commenter instanceof IndentedCommenter) {
            final Boolean value = ((IndentedCommenter)commenter).forceIndentedLineComment();
            if (value != null) {
              block.commentWithIndent = value;
            }
          }
          break;
        }
      }
    }
    boolean moveCarets = true;
    for (Block block : myBlocks) {
      if (block.carets.size() > 1 && block.startLine != block.endLine) {
        moveCarets = false;
        break;
      }
    }
    // third pass - actual change
    Collections.reverse(myBlocks);
    for (Block block : myBlocks) {
      if (!block.skip) {
        if (!allLinesCommented) {
          if (!block.commentWithIndent) {
            doDefaultCommenting(block);
          }
          else {
            doIndentCommenting(block);
          }
        }
        else {
          doUncommenting(block);
        }
      }

      if (!moveCarets || block.caretUpdate == null) {
        continue;
      }
      Document document = block.editor.getDocument();
      for (Caret caret : block.carets) {
        switch (block.caretUpdate) {
          case PUT_AT_COMMENT_START:
            final Commenter commenter = block.commenters[0];
            if (commenter != null) {
              String prefix;
              if (commenter instanceof SelfManagingCommenter) {
                prefix = ((SelfManagingCommenter)commenter).getCommentPrefix(block.startLine,
                                                                             document,
                                                                             block.commenterStateMap.get((SelfManagingCommenter)commenter));
                if (prefix == null) prefix = ""; // TODO
              }
              else {
                prefix = commenter.getLineCommentPrefix();
                if (prefix == null) prefix = commenter.getBlockCommentPrefix();
              }

              int lineStart = document.getLineStartOffset(block.startLine);
              lineStart = CharArrayUtil.shiftForward(document.getCharsSequence(), lineStart, " \t");
              lineStart += prefix.length();
              lineStart = CharArrayUtil.shiftForward(document.getCharsSequence(), lineStart, " \t");
              if (lineStart > document.getTextLength()) lineStart = document.getTextLength();
              caret.moveToOffset(lineStart);
            }
            break;
          case SHIFT_DOWN:
            // Don't tweak caret position if we're already located on the last document line.
            LogicalPosition position = caret.getLogicalPosition();
            if (position.line < document.getLineCount() - 1) {
              int verticalShift = 1 + block.editor.getSoftWrapModel().getSoftWrapsForLine(position.line).size()
                                  - EditorUtil.getSoftWrapCountAfterLineStart(block.editor, position);
              caret.moveCaretRelatively(0, verticalShift, false, true);
            }
            break;
          case RESTORE_SELECTION:
            caret.setSelection(document.getLineStartOffset(document.getLineNumber(caret.getSelectionStart())), caret.getSelectionEnd());
        }
      }
    }
  }

  private static void doUncommenting(Block block) {
    DocumentUtil.executeInBulk(block.editor.getDocument(),
                               block.endLine - block.startLine >= Registry.intValue("comment.by.line.bulk.lines.trigger"),
                               () -> {
                                 for (int line = block.endLine; line >= block.startLine; line--) {
                                   uncommentLine(block, line, block.addSpace);
                                 }
                               });
  }

  private static Commenter getBlockSuitableCommenter(final PsiFile file, int offset, int endOffset) {
    final Language languageSuitableForCompleteFragment;
    if (offset >= endOffset) {  // we are on empty line
      PsiElement element = file.findElementAt(offset);
      if (element != null) languageSuitableForCompleteFragment = element.getParent().getLanguage();
      else languageSuitableForCompleteFragment = null;
    }
    else {
      languageSuitableForCompleteFragment = PsiUtilBase.reallyEvaluateLanguageInRange(offset, endOffset, file);
    }


    Commenter blockSuitableCommenter =
      languageSuitableForCompleteFragment == null ? LanguageCommenters.INSTANCE.forLanguage(file.getLanguage()) : null;
    if (blockSuitableCommenter == null && file.getFileType() instanceof CustomSyntaxTableFileType) {
      blockSuitableCommenter = new Commenter() {
        final SyntaxTable mySyntaxTable = ((CustomSyntaxTableFileType)file.getFileType()).getSyntaxTable();

        @Override
        @Nullable
        public String getLineCommentPrefix() {
          return mySyntaxTable.getLineComment();
        }

        @Override
        @Nullable
        public String getBlockCommentPrefix() {
          return mySyntaxTable.getStartComment();
        }

        @Override
        @Nullable
        public String getBlockCommentSuffix() {
          return mySyntaxTable.getEndComment();
        }

        @Override
        public String getCommentedBlockCommentPrefix() {
          return null;
        }

        @Override
        public String getCommentedBlockCommentSuffix() {
          return null;
        }
      };
    }

    return blockSuitableCommenter;
  }

  private static boolean isLineCommented(Block block, final int line, final Commenter commenter) {
    boolean commented;
    int lineEndForBlockCommenting = -1;
    Document document = block.editor.getDocument();
    int lineStart = document.getLineStartOffset(line);
    CharSequence chars = document.getCharsSequence();
    lineStart = CharArrayUtil.shiftForward(chars, lineStart, " \t");

    if (commenter instanceof SelfManagingCommenter) {
      final SelfManagingCommenter selfManagingCommenter = (SelfManagingCommenter)commenter;
      commented = selfManagingCommenter.isLineCommented(line, lineStart, document, block.commenterStateMap.get(selfManagingCommenter));
    }
    else {
      String prefix = commenter.getLineCommentPrefix();

      if (prefix != null) {
        commented = CharArrayUtil.regionMatches(chars, lineStart, StringUtil.trimTrailing(prefix));
      }
      else {
        prefix = commenter.getBlockCommentPrefix();
        String suffix = commenter.getBlockCommentSuffix();
        final int textLength = document.getTextLength();
        lineEndForBlockCommenting = document.getLineEndOffset(line);
        if (lineEndForBlockCommenting == textLength) {
          final int shifted = CharArrayUtil.shiftBackward(chars, textLength - 1, " \t");
          if (shifted < textLength - 1) lineEndForBlockCommenting = shifted;
        }
        else {
          lineEndForBlockCommenting = CharArrayUtil.shiftBackward(chars, lineEndForBlockCommenting, " \t");
        }
        commented = lineStart == lineEndForBlockCommenting && block.startLine != block.endLine ||
                    CharArrayUtil.regionMatches(chars, lineStart, prefix)
                    && CharArrayUtil.regionMatches(chars, lineEndForBlockCommenting - suffix.length(), suffix);
      }
    }

    if (commented) {
      block.startOffsets[line - block.startLine] = lineStart;
      block.endOffsets[line - block.startLine] = lineEndForBlockCommenting;
    }

    return commented;
  }

  @Nullable
  private static Commenter findCommenter(@NotNull Editor editor, @NotNull PsiFile file, final int line) {
    final FileType fileType = file.getFileType();
    if (fileType instanceof AbstractFileType) {
      return ((AbstractFileType)fileType).getCommenter();
    }
    final Language lineStartLanguage = getLineStartLanguage(editor, file, line);
    final Language lineEndLanguage = getLineEndLanguage(file, editor, line);
    return CommentByBlockCommentHandler.getCommenter(file, editor, lineStartLanguage, lineEndLanguage);
  }

  @NotNull
  private static Language getLineStartLanguage(@NotNull Editor editor, @NotNull PsiFile file, int line) {
    Document document = editor.getDocument();
    int lineStartOffset = document.getLineStartOffset(line);
    lineStartOffset = Math.max(0, CharArrayUtil.shiftForward(document.getCharsSequence(), lineStartOffset, " \t"));
    return PsiUtilCore.getLanguageAtOffset(file, lineStartOffset);
  }

  @NotNull
  private static Language getLineEndLanguage(@NotNull PsiFile file, @NotNull Editor editor, int line) {
    Document document = editor.getDocument();
    int lineEndOffset = document.getLineEndOffset(line) - 1;
    lineEndOffset = Math.max(0, CharArrayUtil.shiftBackward(document.getCharsSequence(), lineEndOffset < 0 ? 0 : lineEndOffset, " \t"));
    return PsiUtilCore.getLanguageAtOffset(file, lineEndOffset);
  }

  private static IndentData computeMinIndent(Editor editor, PsiFile psiFile, int line1, int line2) {
    Document document = editor.getDocument();
    IndentData minIndent = CommentUtil.getMinLineIndent(document, line1, line2, psiFile);
    if (line1 > 0) {
      int commentOffset = getCommentStart(editor, psiFile, line1 - 1);
      if (commentOffset >= 0) {
        int lineStart = document.getLineStartOffset(line1 - 1);
        IndentData indent = IndentData.createFrom(document.getCharsSequence(), lineStart, commentOffset, CodeStyle.getIndentOptions(psiFile).TAB_SIZE);
        minIndent = IndentData.min(minIndent, indent);
      }
    }
    if (minIndent == null) {
      minIndent = new IndentData(0);
    }
    return minIndent;
  }

  private static int getCommentStart(Editor editor, PsiFile psiFile, int line) {
    int offset = editor.getDocument().getLineStartOffset(line);
    CharSequence chars = editor.getDocument().getCharsSequence();
    offset = CharArrayUtil.shiftForward(chars, offset, " \t");
    final Commenter commenter = findCommenter(editor, psiFile, line);
    if (commenter == null) return -1;
    String prefix = commenter.getLineCommentPrefix();
    if (prefix == null) prefix = commenter.getBlockCommentPrefix();
    if (prefix == null) return -1;
    return CharArrayUtil.regionMatches(chars, offset, prefix) ? offset : -1;
  }

  public void doDefaultCommenting(final Block block) {
    final Document document = block.editor.getDocument();
    DocumentUtil.executeInBulk(
      document, block.endLine - block.startLine >= Registry.intValue("comment.by.line.bulk.lines.trigger"), () -> {
        for (int line = block.endLine; line >= block.startLine; line--) {
          int offset = document.getLineStartOffset(line);
          commentLine(block, line, offset);
        }
      });
  }

  private static void doIndentCommenting(final Block block) {
    final Document document = block.editor.getDocument();
    final CharSequence chars = document.getCharsSequence();
    final IndentData minIndent = computeMinIndent(block.editor, block.psiFile, block.startLine, block.endLine);
    final CommonCodeStyleSettings.IndentOptions indentOptions = CodeStyle.getIndentOptions(block.psiFile);

    DocumentUtil.executeInBulk(
      document, block.endLine - block.startLine > Registry.intValue("comment.by.line.bulk.lines.trigger"), () -> {
        for (int line = block.endLine; line >= block.startLine; line--) {
          int lineStart = document.getLineStartOffset(line);
          int offset = lineStart;
          final StringBuilder buffer = new StringBuilder();
          while (true) {
            IndentData indent = IndentData.createFrom(buffer, 0, buffer.length(), indentOptions.TAB_SIZE);
            if (indent.getTotalSpaces() >= minIndent.getTotalSpaces()) break;
            char c = chars.charAt(offset);
            if (c != ' ' && c != '\t') {
              String newSpace = minIndent.createIndentInfo().generateNewWhiteSpace(indentOptions);
              document.replaceString(lineStart, offset, newSpace);
              offset = lineStart + newSpace.length();
              break;
            }
            buffer.append(c);
            offset++;
          }
          commentLine(block, line, offset);
        }
      });
  }

  private static void uncommentRange(Document document, int startOffset, int endOffset, @NotNull Commenter commenter) {
    final String commentedSuffix = commenter.getCommentedBlockCommentSuffix();
    final String commentedPrefix = commenter.getCommentedBlockCommentPrefix();
    final String prefix = commenter.getBlockCommentPrefix();
    final String suffix = commenter.getBlockCommentSuffix();
    if (prefix == null || suffix == null) {
      return;
    }
    if (endOffset >= suffix.length() && CharArrayUtil.regionMatches(document.getCharsSequence(), endOffset - suffix.length(), suffix)) {
      document.deleteString(endOffset - suffix.length(), endOffset);
      endOffset -= suffix.length();
    }
    if (commentedPrefix != null && commentedSuffix != null) {
      CommentByBlockCommentHandler.commentNestedComments(document, new TextRange(startOffset, endOffset), commenter);
    }
    document.deleteString(startOffset, startOffset + prefix.length());
  }

  private static void uncommentLine(Block block, int line, boolean removeSpace) {
    Document document = block.editor.getDocument();
    Commenter commenter = block.commenters[line - block.startLine];
    if (commenter == null) commenter = findCommenter(block.editor, block.psiFile, line);
    if (commenter == null) return;

    final int startOffset = block.startOffsets[line - block.startLine];
    final int endOffset = block.endOffsets[line - block.startLine];
    if (startOffset == endOffset) {
      return;
    }

    if (commenter instanceof SelfManagingCommenter) {
      final SelfManagingCommenter selfManagingCommenter = (SelfManagingCommenter)commenter;
      selfManagingCommenter.uncommentLine(line, startOffset, document, block.commenterStateMap.get(selfManagingCommenter));
      return;
    }

    RangeMarker marker = endOffset > startOffset ? block.editor.getDocument().createRangeMarker(startOffset, endOffset) : null;
    try {
      if (doUncommentLine(line, document, commenter, startOffset, endOffset, removeSpace)) return;
      if (marker != null) {
        CommentByBlockCommentHandler.processDocument(document, marker, commenter, false);
      }
    }
    finally {
      if (marker != null) {
        marker.dispose();
      }
    }
  }

  private static boolean doUncommentLine(int line, Document document, Commenter commenter, int startOffset, int endOffset, boolean removeSpace) {
    String prefix = commenter.getLineCommentPrefix();
    if (prefix != null) {
      int originalPrefixLength = prefix.length();
      if (removeSpace) prefix += ' ';
      CharSequence chars = document.getCharsSequence();

      if (commenter instanceof CommenterWithLineSuffix) {
        CommenterWithLineSuffix commenterWithLineSuffix = (CommenterWithLineSuffix)commenter;
        String suffix = commenterWithLineSuffix.getLineCommentSuffix();


        int theEnd = endOffset > 0 ? endOffset : document.getLineEndOffset(line);
        while (theEnd > startOffset && Character.isWhitespace(chars.charAt(theEnd - 1))) {
          theEnd--;
        }

        int suffixPos = CharArrayUtil.indexOf(chars, suffix, startOffset + originalPrefixLength, theEnd);
        if (suffixPos != -1) {
          document.deleteString(suffixPos, suffixPos + suffix.length());
        }
      }

      boolean matchesTrimmed = false;
      boolean commented = CharArrayUtil.regionMatches(chars, startOffset, prefix) ||
                          (matchesTrimmed = prefix.endsWith(" ") && CharArrayUtil.regionMatches(chars, startOffset, prefix.trim()));
      assert commented : "Commenter: " + commenter;

      int charsToDelete = matchesTrimmed ? prefix.trim().length() : prefix.length();
      document.deleteString(startOffset, startOffset + charsToDelete);

      // delete whitespace on line if that's all that left after uncommenting
      int lineStartOffset = document.getLineStartOffset(line);
      int lineEndOffset = document.getLineEndOffset(line);
      if (CharArrayUtil.isEmptyOrSpaces(chars, lineStartOffset, lineEndOffset)) document.deleteString(lineStartOffset, lineEndOffset);

      return true;
    }
    String text = document.getCharsSequence().subSequence(startOffset, endOffset).toString();

    prefix = commenter.getBlockCommentPrefix();
    final String suffix = commenter.getBlockCommentSuffix();
    if (prefix == null || suffix == null) {
      return true;
    }

    IntArrayList prefixes = new IntArrayList();
    IntArrayList suffixes = new IntArrayList();
    for (int position = 0; position < text.length(); ) {
      int prefixPos = text.indexOf(prefix, position);
      if (prefixPos == -1) {
        break;
      }
      prefixes.add(prefixPos);
      position = prefixPos + prefix.length();
      int suffixPos = text.indexOf(suffix, position);
      if (suffixPos == -1) {
        suffixPos = text.length() - suffix.length();
      }
      suffixes.add(suffixPos);
      position = suffixPos + suffix.length();
    }

    assert prefixes.size() == suffixes.size();

    for (int i = prefixes.size() - 1; i >= 0; i--) {
      uncommentRange(document, startOffset + prefixes.get(i), Math.min(startOffset + suffixes.get(i) + suffix.length(), endOffset), commenter);
    }
    return false;
  }

  private static void commentLine(Block block, int line, int offset) {
    Commenter commenter = block.blockSuitableCommenter;
    Document document = block.editor.getDocument();
    if (commenter == null) commenter = findCommenter(block.editor, block.psiFile, line);
    if (commenter == null) return;
    if (commenter instanceof SelfManagingCommenter) {
      final SelfManagingCommenter selfManagingCommenter = (SelfManagingCommenter)commenter;
      selfManagingCommenter.commentLine(line, offset, document, block.commenterStateMap.get(selfManagingCommenter));
      return;
    }

    int endOffset = document.getLineEndOffset(line);
    RangeMarker marker = document.createRangeMarker(offset, endOffset);
    marker.setGreedyToLeft(true);
    marker.setGreedyToRight(true);
    try {
      if (doCommentLine(block, line, offset, endOffset, commenter, document)) return;
      CommentByBlockCommentHandler.processDocument(document, marker, commenter, true);
    }
    finally {
      marker.dispose();
    }
  }

  private static boolean doCommentLine(Block block,
                                       int line,
                                       int offset,
                                       int endOffset,
                                       Commenter commenter,
                                       Document document) {
    String prefix = commenter.getLineCommentPrefix();
    int shiftedStartOffset = CharArrayUtil.shiftForward(document.getCharsSequence(), offset, " \t");
    if (prefix != null) {
      if (commenter instanceof CommenterWithLineSuffix) {
        endOffset = CharArrayUtil.shiftBackward(document.getCharsSequence(), endOffset, " \t");
        String lineSuffix = ((CommenterWithLineSuffix)commenter).getLineCommentSuffix();
        if (!CharArrayUtil.regionMatches(document.getCharsSequence(), shiftedStartOffset, prefix)) {
          if (!CharArrayUtil.regionMatches(document.getCharsSequence(), endOffset - lineSuffix.length(), lineSuffix)) {
            document.insertString(endOffset, lineSuffix);
          }
          document.insertString(offset, prefix);
        }
      }
      else {
        if (block.addSpace &&
            shiftedStartOffset < document.getTextLength() &&
            document.getCharsSequence().charAt(shiftedStartOffset) != '\n') {
          prefix += ' ';
        }
        document.insertString(offset, prefix);
      }
    }
    else {
      prefix = commenter.getBlockCommentPrefix();
      String suffix = commenter.getBlockCommentSuffix();
      if (prefix == null || suffix == null) return true;
      if (endOffset == offset && block.startLine != block.endLine) return true;
      final int textLength = document.getTextLength();
      final CharSequence chars = document.getCharsSequence();
      if (endOffset == textLength) {
        final int shifted = CharArrayUtil.shiftBackward(chars, textLength - 1, " \t") + 1;
        if (shifted < textLength) endOffset = shifted;
      }
      else {
        endOffset = CharArrayUtil.shiftBackward(chars, endOffset, " \t");
      }
      if (endOffset < offset ||
          offset == textLength - 1 && line != document.getLineCount() - 1) {
        return true;
      }
      final String text = chars.subSequence(offset, endOffset).toString();
      final IntArrayList prefixes = new IntArrayList();
      final IntArrayList suffixes = new IntArrayList();
      final String commentedSuffix = commenter.getCommentedBlockCommentSuffix();
      final String commentedPrefix = commenter.getCommentedBlockCommentPrefix();
      for (int position = 0; position < text.length(); ) {
        int nearestPrefix = text.indexOf(prefix, position);
        if (nearestPrefix == -1) {
          nearestPrefix = text.length();
        }
        int nearestSuffix = text.indexOf(suffix, position);
        if (nearestSuffix == -1) {
          nearestSuffix = text.length();
        }
        if (Math.min(nearestPrefix, nearestSuffix) == text.length()) {
          break;
        }
        if (nearestPrefix < nearestSuffix) {
          prefixes.add(nearestPrefix);
          position = nearestPrefix + prefix.length();
        }
        else {
          suffixes.add(nearestSuffix);
          position = nearestSuffix + suffix.length();
        }
      }
      if (!(commentedSuffix == null && !suffixes.isEmpty() && offset + suffixes.get(suffixes.size() - 1) + suffix.length() >= endOffset)) {
        document.insertString(endOffset, suffix);
      }
      int nearestPrefix = prefixes.size() - 1;
      int nearestSuffix = suffixes.size() - 1;
      while (nearestPrefix >= 0 || nearestSuffix >= 0) {
        if (nearestSuffix == -1 || nearestPrefix != -1 && prefixes.get(nearestPrefix) > suffixes.get(nearestSuffix)) {
          final int position = prefixes.get(nearestPrefix);
          nearestPrefix--;
          if (commentedPrefix != null) {
            document.replaceString(offset + position, offset + position + prefix.length(), commentedPrefix);
          }
          else if (position != 0) {
            document.insertString(offset + position, suffix);
          }
        }
        else {
          final int position = suffixes.get(nearestSuffix);
          nearestSuffix--;
          if (commentedSuffix != null) {
            document.replaceString(offset + position, offset + position + suffix.length(), commentedSuffix);
          }
          else if (offset + position + suffix.length() < endOffset) {
            document.insertString(offset + position + suffix.length(), prefix);
          }
        }
      }
      if (!(commentedPrefix == null && !prefixes.isEmpty() && prefixes.get(0) == 0)) {
        document.insertString(offset, prefix);
      }
    }
    return false;
  }

  private static class Block {
    private Editor editor;
    private PsiFile psiFile;
    private final List<Caret> carets = new ArrayList<>();
    private int startLine;
    private int endLine;
    private int[] startOffsets;
    private int[] endOffsets;
    private Commenter blockSuitableCommenter;
    private Commenter[] commenters;
    private Map<SelfManagingCommenter, CommenterDataHolder> commenterStateMap;
    private boolean commentWithIndent;
    private CaretUpdate caretUpdate;
    private boolean skip;
    private boolean addSpace;
  }

  private enum CaretUpdate {
    PUT_AT_COMMENT_START, SHIFT_DOWN, RESTORE_SELECTION
  }
}

/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.codeInsight.editorActions.moveUpDown;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class MoverWrapper {
  private static final Logger LOGGER = Logger.getInstance(MoverWrapper.class);
  
  protected final boolean myIsDown;
  private final StatementUpDownMover myMover;
  private final StatementUpDownMover.MoveInfo myInfo;

  protected MoverWrapper(@NotNull final StatementUpDownMover mover, @NotNull final StatementUpDownMover.MoveInfo info, final boolean isDown) {
    myMover = mover;
    myIsDown = isDown;

    myInfo = info;
  }

  public StatementUpDownMover.MoveInfo getInfo() {
    return myInfo;
  }

  public final void move(final Editor editor, final PsiFile file) {
    assert myInfo.toMove2 != null;
    myMover.beforeMove(editor, myInfo, myIsDown);
    final Document document = editor.getDocument();
    final Project project = file.getProject();
    if (!myInfo.toMove.equals(myInfo.toMove2)) { // some movers (e.g. PyStatementMover) perform actual moving inside beforeMove/afterMove
      final int start = StatementUpDownMover.getLineStartSafeOffset(document, myInfo.toMove.startLine);
      final int end = StatementUpDownMover.getLineStartSafeOffset(document, myInfo.toMove.endLine);
      String textToInsert = document.getCharsSequence().subSequence(start, end).toString();
      if (!StringUtil.endsWithChar(textToInsert,'\n')) textToInsert += '\n';

      final int start2 = document.getLineStartOffset(myInfo.toMove2.startLine);
      final int end2 = StatementUpDownMover.getLineStartSafeOffset(document,myInfo.toMove2.endLine);
      String textToInsert2 = document.getCharsSequence().subSequence(start2, end2).toString();
      if (!StringUtil.endsWithChar(textToInsert2,'\n')) textToInsert2 += '\n';

      myInfo.range1 = document.createRangeMarker(start, end);
      myInfo.range2 = document.createRangeMarker(start2, end2);
      if (myInfo.range1.getStartOffset() < myInfo.range2.getStartOffset()) {
        myInfo.range1.setGreedyToLeft(true);
        myInfo.range1.setGreedyToRight(false);
        myInfo.range2.setGreedyToLeft(true);
        myInfo.range2.setGreedyToRight(true);
      }
      else {
        myInfo.range1.setGreedyToLeft(true);
        myInfo.range1.setGreedyToRight(true);
        myInfo.range2.setGreedyToLeft(true);
        myInfo.range2.setGreedyToRight(false);
      }

      TextRange range = new TextRange(start, end);
      TextRange range2 = new TextRange(start2, end2);
      if (!range.equals(range2)) {
        if (range.intersectsStrict(range2)) {
          LOGGER.error("Wrong move ranges requested by " + myMover + " " + start + ":" + end + " vs " + start2 + ":" + end2,
                       new Attachment("ranges.txt",
                                      start + ":" + end + "(" + textToInsert + ")\n" + start2 + ":" + end2 + "(" + textToInsert2 + ")"));
          return;
        }

        final CaretModel caretModel = editor.getCaretModel();
        final int caretRelativePos = caretModel.getOffset() - start;
        final SelectionModel selectionModel = editor.getSelectionModel();
        final int selectionStart = selectionModel.getSelectionStart();
        final int selectionEnd = selectionModel.getSelectionEnd();
        final boolean hasSelection = selectionModel.hasSelection();

        // to prevent flicker
        caretModel.moveToOffset(0);

        // There is a possible case that the user performs, say, method move. It's also possible that one (or both) of moved methods
        // are folded. We want to preserve their states then. The problem is that folding processing is based on PSI element pointers
        // and the pointers behave as following during move up/down:
        //     method1() {}
        //     method2() {}
        // Pointer for the fold region from method1 points to 'method2()' now and vice versa (check range markers processing on
        // document change for further information). I.e. information about fold regions statuses holds the data swapped for
        // 'method1' and 'method2'. Hence, we want to apply correct 'collapsed' status.
        final FoldRegion topRegion = findTopLevelRegionInRange(editor, myInfo.range1);
        final FoldRegion bottomRegion = findTopLevelRegionInRange(editor, myInfo.range2);

        if (document instanceof DocumentEx) {
          int startFirst = Math.min(start, start2);
          int endFirst = Math.min(end, end2);
          int startSecond = Math.max(start, start2);
          int endSecond = Math.max(end, end2);
          ((DocumentEx)document).moveText(startFirst, endFirst, startSecond);
          ((DocumentEx)document).moveText(startSecond, endSecond, startFirst);
          myInfo.range1.dispose();
          myInfo.range2.dispose();
          // we could use existing range markers, but if some range is empty, they won't be moved as expected
          myInfo.range1 = document.createRangeMarker(start < start2 ? start                 : start2 + end - end2,
                                                     start < start2 ? start + end2 - start2 : end);
          myInfo.range2 = document.createRangeMarker(start < start2 ? start + end2 - end    : start2,
                                                     start < start2 ? end2                  : start2 + end - start);
          insertLineBreakInTheEndIfMissing(myInfo.range1);
          insertLineBreakInTheEndIfMissing(myInfo.range2);
        }
        else {
          document.insertString(myInfo.range1.getStartOffset(), textToInsert2);
          document.deleteString(myInfo.range1.getStartOffset()+textToInsert2.length(), myInfo.range1.getEndOffset());

          document.insertString(myInfo.range2.getStartOffset(), textToInsert);
          int s = myInfo.range2.getStartOffset() + textToInsert.length();
          int e = myInfo.range2.getEndOffset();
          if (e > s) {
            document.deleteString(s, e);
          }
        }

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        // Swap fold regions status if necessary.
        if (topRegion != null && bottomRegion != null) {
          CodeFoldingManager.getInstance(project).updateFoldRegions(editor);
          editor.getFoldingModel().runBatchFoldingOperation(() -> {
            FoldRegion newTopRegion = findTopLevelRegionInRange(editor, myInfo.range1);
            if (newTopRegion != null) {
              newTopRegion.setExpanded(bottomRegion.isExpanded());
            }

            FoldRegion newBottomRegion = findTopLevelRegionInRange(editor, myInfo.range2);
            if (newBottomRegion != null) {
              newBottomRegion.setExpanded(topRegion.isExpanded());
            }
          });
        }

        if (hasSelection) {
          restoreSelection(editor, selectionStart, selectionEnd, start, end, myInfo.range2.getStartOffset());
        }

        caretModel.moveToOffset(myInfo.range2.getStartOffset() + caretRelativePos);
      }
    }
    myMover.afterMove(editor, file, myInfo, myIsDown);
    PsiDocumentManager.getInstance(project).commitDocument(document);
    if (myInfo.indentTarget) {
      indentLinesIn(editor, file, document, project, myInfo.range2);
    }
    if (myInfo.indentSource) {
      indentLinesIn(editor, file, document, project, myInfo.range1);
    }

    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
  }

  private static void insertLineBreakInTheEndIfMissing(@NotNull RangeMarker marker) {
    Document document = marker.getDocument();
    int startOffset = marker.getStartOffset();
    int endOffset = marker.getEndOffset();
    if (startOffset == endOffset || document.getImmutableCharSequence().charAt(endOffset - 1) != '\n') {
      marker.setGreedyToRight(true);
      document.insertString(endOffset, "\n");
    }
  }

  private static FoldRegion findTopLevelRegionInRange(Editor editor, RangeMarker range) {
    FoldRegion result = null;
    for (FoldRegion foldRegion : editor.getFoldingModel().getAllFoldRegions()) {
      if (foldRegion.isValid() && contains(range, foldRegion) && !contains(result, foldRegion)) {
        result = foldRegion;
      }
    }
    return result;
  }

  /**
   * Allows to check if text range defined by the given range marker completely contains text range of the given fold region.
   *
   * @param rangeMarker   range marker to check
   * @param foldRegion    fold region to check
   * @return              {@code true} if text range defined by the given range marker completely contains text range
   *                      of the given fold region; {@code false} otherwise
   */
  private static boolean contains(@NotNull RangeMarker rangeMarker, @NotNull FoldRegion foldRegion) {
    return rangeMarker.getStartOffset() <= foldRegion.getStartOffset() && rangeMarker.getEndOffset() >= foldRegion.getEndOffset();
  }

  /**
   * Allows to check if given {@code 'region2'} is nested to {@code 'region1'}
   *
   * @param region1   'outer' region candidate
   * @param region2   'inner' region candidate
   * @return          {@code true} if 'region2' is nested to 'region1'; {@code false} otherwise
   */
  private static boolean contains(@Nullable FoldRegion region1, @NotNull FoldRegion region2) {
    if (region1 == null) {
      return false;
    }
    return region1.getStartOffset() <= region2.getStartOffset() && region1.getEndOffset() >= region2.getEndOffset();
  }

  private static void indentLinesIn(final Editor editor, final PsiFile file, final Document document, final Project project, RangeMarker range) {
    final CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
    int line1 = editor.offsetToLogicalPosition(range.getStartOffset()).line;
    int line2 = editor.offsetToLogicalPosition(range.getEndOffset()).line;

    while (!lineContainsNonSpaces(document, line1) && line1 < line2) line1++;
    while (!lineContainsNonSpaces(document, line2) && line1 < line2) line2--;

    final FileViewProvider provider = file.getViewProvider();
    PsiFile rootToAdjustIndentIn = provider.getPsi(provider.getBaseLanguage());
    codeStyleManager.adjustLineIndent(rootToAdjustIndentIn, new TextRange(document.getLineStartOffset(line1), document.getLineStartOffset(line2)));
  }

  private static boolean lineContainsNonSpaces(final Document document, final int line) {
    if (line >= document.getLineCount()) {
      return false;
    }
    int lineStartOffset = document.getLineStartOffset(line);
    int lineEndOffset = document.getLineEndOffset(line);
    @NonNls String text = document.getCharsSequence().subSequence(lineStartOffset, lineEndOffset).toString();
    return text.trim().length() != 0;
  }

  private static void restoreSelection(Editor editor,
                                       int selectionStart, int selectionEnd, int moveStartOffset, int moveEndOffset, int insOffset) {
    int selectionRelativeStartOffset = Math.max(0, selectionStart - moveStartOffset);
    int selectionRelativeEndOffset = Math.min(moveEndOffset - moveStartOffset, selectionEnd - moveStartOffset);
    int newSelectionStart = insOffset + selectionRelativeStartOffset;
    int newSelectionEnd = insOffset + selectionRelativeEndOffset;
    EditorUtil.setSelectionExpandingFoldedRegionsIfNeeded(editor, newSelectionStart, newSelectionEnd);
  }
}

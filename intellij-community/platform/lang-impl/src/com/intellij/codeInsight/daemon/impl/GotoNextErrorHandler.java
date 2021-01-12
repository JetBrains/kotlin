// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.analysis.problemsView.toolWindow.ProblemsView.selectHighlighterIfVisible;

public class GotoNextErrorHandler implements CodeInsightActionHandler {
  private final boolean myGoForward;

  public GotoNextErrorHandler(boolean goForward) {
    myGoForward = goForward;
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    int caretOffset = editor.getCaretModel().getOffset();
    gotoNextError(project, editor, file, caretOffset);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  private void gotoNextError(Project project, Editor editor, PsiFile file, int caretOffset) {
    final SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
    DaemonCodeAnalyzerSettings settings = DaemonCodeAnalyzerSettings.getInstance();
    int maxSeverity = settings.isNextErrorActionGoesToErrorsFirst() ? severityRegistrar.getSeveritiesCount() - 1
                                                                    : SeverityRegistrar.SHOWN_SEVERITIES_OFFSET;

    for (int idx = maxSeverity; idx >= SeverityRegistrar.SHOWN_SEVERITIES_OFFSET; idx--) {
      final HighlightSeverity minSeverity = severityRegistrar.getSeverityByIndex(idx);
      HighlightInfo infoToGo = findInfo(project, editor, caretOffset, minSeverity);
      if (infoToGo != null) {
        navigateToError(project, editor, infoToGo, () -> {
          if (Registry.is("error.navigation.show.tooltip")) {
            // When there are multiple warnings at the same offset, this will return the HighlightInfo
            // containing all of them, not just the first one as found by findInfo()
            HighlightInfo fullInfo = ((DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project))
              .findHighlightByOffset(editor.getDocument(), editor.getCaretModel().getOffset(), false);
            DaemonTooltipUtil.showInfoTooltip(fullInfo != null ? fullInfo : infoToGo,
                                              editor, editor.getCaretModel().getOffset(), false, true);
          }
        });
        return;
      }
    }
    showMessageWhenNoHighlights(project, file, editor);
  }

  private HighlightInfo findInfo(Project project, Editor editor, final int caretOffset, HighlightSeverity minSeverity) {
    final Document document = editor.getDocument();
    final HighlightInfo[][] infoToGo = new HighlightInfo[2][2]; //HighlightInfo[luck-noluck][skip-noskip]
    final int caretOffsetIfNoLuck = myGoForward ? -1 : document.getTextLength();

    DaemonCodeAnalyzerEx.processHighlights(document, project, minSeverity, 0, document.getTextLength(), info -> {
      int startOffset = getNavigationPositionFor(info, document);
      if (SeverityRegistrar.isGotoBySeverityEnabled(info.getSeverity())) {
        infoToGo[0][0] = getBetterInfoThan(infoToGo[0][0], caretOffset, startOffset, info);
        infoToGo[1][0] = getBetterInfoThan(infoToGo[1][0], caretOffsetIfNoLuck, startOffset, info);
      }
      infoToGo[0][1] = getBetterInfoThan(infoToGo[0][1], caretOffset, startOffset, info);
      infoToGo[1][1] = getBetterInfoThan(infoToGo[1][1], caretOffsetIfNoLuck, startOffset, info);
      return true;
    });
    if (infoToGo[0][0] == null) infoToGo[0][0] = infoToGo[1][0];
    if (infoToGo[0][1] == null) infoToGo[0][1] = infoToGo[1][1];
    if (infoToGo[0][0] == null) infoToGo[0][0] = infoToGo[0][1];
    return infoToGo[0][0];
  }

  private HighlightInfo getBetterInfoThan(HighlightInfo infoToGo, int caretOffset, int startOffset, HighlightInfo info) {
    if (isBetterThan(infoToGo, caretOffset, startOffset)) {
      infoToGo = info;
    }
    return infoToGo;
  }

  private boolean isBetterThan(HighlightInfo oldInfo, int caretOffset, int newOffset) {
    if (oldInfo == null) return true;
    int oldOffset = getNavigationPositionFor(oldInfo, oldInfo.getHighlighter().getDocument());
    if (myGoForward) {
      return caretOffset < oldOffset != caretOffset < newOffset ? caretOffset < newOffset : newOffset < oldOffset;
    }
    else {
      return caretOffset <= oldOffset != caretOffset <= newOffset ? caretOffset > newOffset : newOffset > oldOffset;
    }
  }

  private static void showMessageWhenNoHighlights(Project project, PsiFile file, Editor editor) {
    DaemonCodeAnalyzerImpl codeHighlighter = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project);
    String message = codeHighlighter.isErrorAnalyzingFinished(file)
                     ? InspectionsBundle.message("no.errors.found.in.this.file")
                     : InspectionsBundle.message("error.analysis.is.in.progress");
    HintManager.getInstance().showInformationHint(editor, message);
  }

  static void navigateToError(@NotNull Project project, @NotNull Editor editor, @NotNull HighlightInfo info, @Nullable Runnable postNavigateRunnable) {
    int oldOffset = editor.getCaretModel().getOffset();

    final int offset = getNavigationPositionFor(info, editor.getDocument());
    final int endOffset = info.getActualEndOffset();

    final ScrollingModel scrollingModel = editor.getScrollingModel();
    if (offset != oldOffset) {
      ScrollType scrollType = offset > oldOffset ? ScrollType.CENTER_DOWN : ScrollType.CENTER_UP;
      editor.getSelectionModel().removeSelection();
      editor.getCaretModel().removeSecondaryCarets();
      editor.getCaretModel().moveToOffset(offset);
      scrollingModel.scrollToCaret(scrollType);
      FoldRegion regionAtOffset = editor.getFoldingModel().getCollapsedRegionAtOffset(offset);
      if (regionAtOffset != null) editor.getFoldingModel().runBatchFoldingOperation(() -> regionAtOffset.setExpanded(true));
    }

    scrollingModel.runActionOnScrollingFinished(
      () -> {
        int maxOffset = editor.getDocument().getTextLength() - 1;
        if (maxOffset == -1) return;
        scrollingModel.scrollTo(editor.offsetToLogicalPosition(Math.min(maxOffset, endOffset)), ScrollType.MAKE_VISIBLE);
        scrollingModel.scrollTo(editor.offsetToLogicalPosition(Math.min(maxOffset, offset)), ScrollType.MAKE_VISIBLE);

        if (postNavigateRunnable != null) {
          postNavigateRunnable.run();
        }
      }
    );

    IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
    RangeHighlighterEx highlighter = info.getHighlighter();
    if (highlighter != null) selectHighlighterIfVisible(project, highlighter);
  }

  private static int getNavigationPositionFor(HighlightInfo info, Document document) {
    int start = info.getActualStartOffset();
    if (start >= document.getTextLength()) return document.getTextLength();
    char c = document.getCharsSequence().charAt(start);
    int shift = info.isAfterEndOfLine() && c != '\n' ? 1 : info.navigationShift;

    int offset = info.getActualStartOffset() + shift;
    return Math.min(offset, document.getTextLength());
  }
}

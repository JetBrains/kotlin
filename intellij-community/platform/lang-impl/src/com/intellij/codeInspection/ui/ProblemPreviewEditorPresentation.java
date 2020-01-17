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
package com.intellij.codeInspection.ui;

import com.intellij.codeInspection.ProblemDescriptorBase;
import com.intellij.diff.tools.util.FoldingModelSupport;
import com.intellij.diff.util.DiffDrawUtil;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.impl.UsagePreviewPanel;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ProblemPreviewEditorPresentation {
  private static final int VIEW_ADDITIONAL_OFFSET = 4;
  private static final int SHOWN_LINES_COUNT = 2;

  static void setupFoldingsAndHighlightProblems(@NotNull EditorEx editor, @NotNull InspectionResultsView view) {
    List<UsageInfo> usages = Arrays.stream(view.getTree().getAllValidSelectedDescriptors())
      .filter(ProblemDescriptorBase.class::isInstance)
      .map(ProblemDescriptorBase.class::cast)
      .map(d -> {
        final PsiElement psi = d.getPsiElement();
        if (psi == null) {
          return null;
        }
        final TextRange range = d.getTextRangeInElement();
        return range == null ? new UsageInfo(psi) : new UsageInfo(psi, range.getStartOffset(), range.getEndOffset());
      })
      .collect(Collectors.toList());
    setupFoldingsAndHighlightProblems(editor, view, usages, view.getProject());
  }


  public static void setupFoldingsAndHighlightProblems(@NotNull EditorEx editor, @NotNull Container editorContainer,
                                                       @NotNull List<? extends UsageInfo> usages, @NotNull Project project) {
    final Document doc = editor.getDocument();
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(project);
    if (documentManager.isUncommited(doc)) {
      WriteAction.run(() -> documentManager.commitDocument(doc));
    }
    if (usages.size() > 1) {
      SortedSet<PreviewEditorFoldingRegion> foldingRegions = new TreeSet<>();
      foldingRegions.add(new PreviewEditorFoldingRegion(0, doc.getLineCount()));
      boolean isUpdated = false;
      for (UsageInfo usage : usages) {
        if (usage == null) {
          return;
        }
        PsiElement element = usage.getElement();
        Segment segment = usage.getSegment();
        assert element != null;
        assert segment != null;
        isUpdated |= makeVisible(foldingRegions, injectedLanguageManager.injectedToHost(element, TextRange.create(segment)), doc);
      }
      if (isUpdated) {
        setupFoldings(editor, foldingRegions);
      }
    }

    highlightProblems(editor, editorContainer, usages, project);
  }

  private static void highlightProblems(EditorEx editor, Container editorContainer, List<? extends UsageInfo> usages, @NotNull Project project) {
    List<UsageInfo> validUsages = ContainerUtil.filter(usages, Objects::nonNull);
    InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(project);
    PsiDocumentManager.getInstance(project).performLaterWhenAllCommitted(() -> {
      if (!editor.isDisposed()) {
        editorContainer.invalidate();
        editorContainer.validate();
        UsagePreviewPanel.highlight(validUsages, editor, project, false, HighlighterLayer.SELECTION);
        if (validUsages.size() == 1) {
          UsageInfo usage = validUsages.get(0);
          final PsiElement element = usage.getElement();
          Segment range = usage.getNavigationRange();
          if (element != null && range != null) {
            if (injectedLanguageManager.getInjectionHost(element) != null) {
              range = injectedLanguageManager.injectedToHost(element, new TextRange(range.getStartOffset(), range.getEndOffset()));
            }
            final Document document = editor.getDocument();
            final int offset = Math.min(range.getEndOffset() + VIEW_ADDITIONAL_OFFSET,
                                        document.getLineEndOffset(document.getLineNumber(range.getEndOffset())));
            editor.getScrollingModel().scrollTo(editor.offsetToLogicalPosition(offset), ScrollType.CENTER);
            return;
          }
        }
        editor.getScrollingModel().scrollTo(editor.offsetToLogicalPosition(0), ScrollType.CENTER_UP);
      }
    });
  }

  public static void setupFoldings(EditorEx editor, SortedSet<? extends PreviewEditorFoldingRegion> foldingRegions) {
    editor.getFoldingModel().runBatchFoldingOperation(() -> {
      editor.getFoldingModel().clearFoldRegions();
      editor.getMarkupModel().removeAllHighlighters();
      for (PreviewEditorFoldingRegion region : foldingRegions) {
        if (region.getEndLine() - region.getStartLine() > 1) {
          FoldRegion currentRegion = FoldingModelSupport.addFolding(editor,
                                                                    region.getStartLine(),
                                                                    region.getEndLine(),
                                                                    false);
          if (currentRegion != null) {
            DiffDrawUtil.createLineSeparatorHighlighter(editor,
                                                        editor.getDocument().getLineStartOffset(region.getStartLine()),
                                                        editor.getDocument().getLineEndOffset(region.getEndLine() - 1),
                                                        () -> currentRegion.isValid() && !currentRegion.isExpanded());
          }
        }
      }
    });
  }

  private static boolean makeVisible(SortedSet<PreviewEditorFoldingRegion> foldingRegions, Segment toShowRange, Document document) {
    if (toShowRange == null) return false;
    boolean isUpdated = false;
    final int startLine = Math.max(0, document.getLineNumber(toShowRange.getStartOffset()) - SHOWN_LINES_COUNT);
    final int endLine = Math.min(document.getLineCount(), document.getLineNumber(toShowRange.getEndOffset()) + SHOWN_LINES_COUNT + 1);
    for (PreviewEditorFoldingRegion range : new ArrayList<>(foldingRegions)) {
      final boolean startInRegion = range.contain(startLine);
      final boolean endInRegion = range.contain(endLine);
      if (startInRegion && endInRegion) {
        foldingRegions.remove(range);
        if (range.getStartLine() != startLine) {
          foldingRegions.add(new PreviewEditorFoldingRegion(range.getStartLine(), startLine));
        }
        if (endLine != range.getEndLine()) {
          foldingRegions.add(new PreviewEditorFoldingRegion(endLine, range.getEndLine()));
        }
        return true;
      }
      if (startInRegion) {
        foldingRegions.remove(range);
        if (range.getStartLine() != startLine) {
          foldingRegions.add(new PreviewEditorFoldingRegion(range.getStartLine(), startLine));
        }
        isUpdated = true;
      }
      if (endInRegion) {
        foldingRegions.remove(range);
        if (endLine != range.getEndLine()) {
          foldingRegions.add(new PreviewEditorFoldingRegion(endLine, range.getEndLine()));
        }
        return true;
      }
    }
    return isUpdated;
  }
}

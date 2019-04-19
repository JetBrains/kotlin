// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.highlighting;

import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.ui.ColorUtil;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

public class HighlightManagerImpl extends HighlightManager {
  private final Project myProject;

  public HighlightManagerImpl(Project project, ActionManagerEx actionManagerEx, final EditorFactory editorFactory) {
    myProject = project;
    ApplicationManager.getApplication().getMessageBus().connect(myProject).subscribe(AnActionListener.TOPIC, new MyAnActionListener());

    DocumentListener documentListener = new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent event) {
        Document document = event.getDocument();
        Editor[] editors = EditorFactory.getInstance().getEditors(document);
        for (Editor editor : editors) {
          Map<RangeHighlighter, HighlightInfo> map = getHighlightInfoMap(editor, false);
          if (map == null) return;

          ArrayList<RangeHighlighter> highlightersToRemove = new ArrayList<>();
          for (RangeHighlighter highlighter : map.keySet()) {
            HighlightInfo info = map.get(highlighter);
            if (!info.editor.getDocument().equals(document)) continue;
            if (BitUtil.isSet(info.flags, HIDE_BY_TEXT_CHANGE)) {
              highlightersToRemove.add(highlighter);
            }
          }

          for (RangeHighlighter highlighter : highlightersToRemove) {
            removeSegmentHighlighter(editor, highlighter);
          }
        }
      }
    };
    editorFactory.getEventMulticaster().addDocumentListener(documentListener, myProject);
  }

  @Nullable
  private Map<RangeHighlighter, HighlightInfo> getHighlightInfoMap(@NotNull Editor editor, boolean toCreate) {
    if (editor instanceof EditorWindow) {
      editor = ((EditorWindow)editor).getDelegate();
    }
    Map<RangeHighlighter, HighlightInfo> map = editor.getUserData(HIGHLIGHT_INFO_MAP_KEY);
    if (map == null && toCreate) {
      map = ((UserDataHolderEx)editor).putUserDataIfAbsent(HIGHLIGHT_INFO_MAP_KEY, new HashMap<>());
    }
    return map;
  }

  @NotNull
  public RangeHighlighter[] getHighlighters(@NotNull Editor editor) {
    Map<RangeHighlighter, HighlightInfo> highlightersMap = getHighlightInfoMap(editor, false);
    if (highlightersMap == null) return RangeHighlighter.EMPTY_ARRAY;
    Set<RangeHighlighter> set = new HashSet<>();
    for (Map.Entry<RangeHighlighter, HighlightInfo> entry : highlightersMap.entrySet()) {
      HighlightInfo info = entry.getValue();
      if (info.editor.equals(editor)) set.add(entry.getKey());
    }
    return set.toArray(RangeHighlighter.EMPTY_ARRAY);
  }

  private RangeHighlighter addSegmentHighlighter(@NotNull Editor editor, int startOffset, int endOffset, TextAttributes attributes, @HideFlags int flags) {
    RangeHighlighter highlighter = editor.getMarkupModel()
      .addRangeHighlighter(startOffset, endOffset, HighlighterLayer.SELECTION - 1, attributes, HighlighterTargetArea.EXACT_RANGE);
    HighlightInfo info = new HighlightInfo(editor instanceof EditorWindow ? ((EditorWindow)editor).getDelegate() : editor, flags);
    Map<RangeHighlighter, HighlightInfo> map = getHighlightInfoMap(editor, true);
    map.put(highlighter, info);
    return highlighter;
  }

  @Override
  public boolean removeSegmentHighlighter(@NotNull Editor editor, @NotNull RangeHighlighter highlighter) {
    Map<RangeHighlighter, HighlightInfo> map = getHighlightInfoMap(editor, false);
    if (map == null) return false;
    HighlightInfo info = map.get(highlighter);
    if (info == null) return false;
    MarkupModel markupModel = info.editor.getMarkupModel();
    if (((MarkupModelEx)markupModel).containsHighlighter(highlighter)) {
      highlighter.dispose();
    }
    map.remove(highlighter);
    return true;
  }

  @Override
  public void addOccurrenceHighlights(@NotNull Editor editor,
                                      @NotNull PsiReference[] occurrences,
                                      @NotNull TextAttributes attributes,
                                      boolean hideByTextChange,
                                      Collection<RangeHighlighter> outHighlighters) {
    if (occurrences.length == 0) return;
    int flags = HIDE_BY_ESCAPE;
    if (hideByTextChange) {
      flags |= HIDE_BY_TEXT_CHANGE;
    }
    Color scrollMarkColor = getScrollMarkColor(attributes, editor.getColorsScheme());

    int oldOffset = editor.getCaretModel().getOffset();
    int horizontalScrollOffset = editor.getScrollingModel().getHorizontalScrollOffset();
    int verticalScrollOffset = editor.getScrollingModel().getVerticalScrollOffset();
    for (PsiReference occurrence : occurrences) {
      PsiElement element = occurrence.getElement();
      int startOffset = element.getTextRange().getStartOffset();
      int start = startOffset + occurrence.getRangeInElement().getStartOffset();
      int end = startOffset + occurrence.getRangeInElement().getEndOffset();
      PsiFile containingFile = element.getContainingFile();
      Project project = element.getProject();
      // each reference can reside in its own injected editor
      Editor textEditor = InjectedLanguageUtil.openEditorFor(containingFile, project);
      if (textEditor != null) {
        addOccurrenceHighlight(textEditor, start, end, attributes, flags, outHighlighters, scrollMarkColor);
      }
    }
    editor.getCaretModel().moveToOffset(oldOffset);
    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    editor.getScrollingModel().scrollHorizontally(horizontalScrollOffset);
    editor.getScrollingModel().scrollVertically(verticalScrollOffset);
  }

  @Override
  public void addOccurrenceHighlight(@NotNull Editor editor,
                                     int start,
                                     int end,
                                     TextAttributes attributes,
                                     int flags,
                                     Collection<RangeHighlighter> outHighlighters,
                                     Color scrollMarkColor) {
    RangeHighlighter highlighter = addSegmentHighlighter(editor, start, end, attributes, flags);
    if (highlighter instanceof RangeHighlighterEx) ((RangeHighlighterEx)highlighter).setVisibleIfFolded(true);
    if (outHighlighters != null) {
      outHighlighters.add(highlighter);
    }
    if (scrollMarkColor != null) {
      highlighter.setErrorStripeMarkColor(scrollMarkColor);
    }
  }

  @Override
  public void addRangeHighlight(@NotNull Editor editor,
                                int startOffset,
                                int endOffset,
                                @NotNull TextAttributes attributes,
                                boolean hideByTextChange,
                                @Nullable Collection<RangeHighlighter> highlighters) {
    addRangeHighlight(editor, startOffset, endOffset, attributes, hideByTextChange, false, highlighters);
  }

  @Override
  public void addRangeHighlight(@NotNull Editor editor,
                                int startOffset,
                                int endOffset,
                                @NotNull TextAttributes attributes,
                                boolean hideByTextChange,
                                boolean hideByAnyKey,
                                @Nullable Collection<RangeHighlighter> highlighters) {
    int flags = HIDE_BY_ESCAPE;
    if (hideByTextChange) {
      flags |= HIDE_BY_TEXT_CHANGE;
    }
    if (hideByAnyKey) {
      flags |= HIDE_BY_ANY_KEY;
    }

    Color scrollMarkColor = getScrollMarkColor(attributes, editor.getColorsScheme());

    addOccurrenceHighlight(editor, startOffset, endOffset, attributes, flags, highlighters, scrollMarkColor);
  }

  @Override
  public void addOccurrenceHighlights(@NotNull Editor editor,
                                      @NotNull PsiElement[] elements,
                                      @NotNull TextAttributes attributes,
                                      boolean hideByTextChange,
                                      Collection<RangeHighlighter> outHighlighters) {
    if (elements.length == 0) return;
    int flags = HIDE_BY_ESCAPE;
    if (hideByTextChange) {
      flags |= HIDE_BY_TEXT_CHANGE;
    }

    Color scrollMarkColor = getScrollMarkColor(attributes, editor.getColorsScheme());
    if (editor instanceof EditorWindow) {
      editor = ((EditorWindow)editor).getDelegate();
    }

    for (PsiElement element : elements) {
      TextRange range = element.getTextRange();
      range = InjectedLanguageManager.getInstance(myProject).injectedToHost(element, range);
      addOccurrenceHighlight(editor,
                             trimOffsetToDocumentSize(editor, range.getStartOffset()),
                             trimOffsetToDocumentSize(editor, range.getEndOffset()),
                             attributes, flags, outHighlighters, scrollMarkColor);
    }
  }

  private static int trimOffsetToDocumentSize(@NotNull Editor editor, int offset) {
    if (offset < 0) return 0;
    int textLength = editor.getDocument().getTextLength();
    return offset < textLength ? offset : textLength;
  }

  @Nullable
  private static Color getScrollMarkColor(@NotNull TextAttributes attributes, @NotNull EditorColorsScheme colorScheme) {
    if (attributes.getErrorStripeColor() != null) return attributes.getErrorStripeColor();
    if (attributes.getBackgroundColor() != null) {
      boolean isDark = ColorUtil.isDark(colorScheme.getDefaultBackground());
      return isDark ? attributes.getBackgroundColor().brighter() : attributes.getBackgroundColor().darker();
    }
    return null;
  }

  public boolean hideHighlights(@NotNull Editor editor, @HideFlags int mask) {
    Map<RangeHighlighter, HighlightInfo> map = getHighlightInfoMap(editor, false);
    if (map == null) return false;

    boolean done = false;
    List<RangeHighlighter> highlightersToRemove = new ArrayList<>();
    for (RangeHighlighter highlighter : map.keySet()) {
      HighlightInfo info = map.get(highlighter);
      if (!info.editor.equals(editor)) continue;
      if ((info.flags & mask) != 0) {
        highlightersToRemove.add(highlighter);
        done = true;
      }
    }

    for (RangeHighlighter highlighter : highlightersToRemove) {
      removeSegmentHighlighter(editor, highlighter);
    }

    return done;
  }

  boolean hasHideByEscapeHighlighters(@NotNull Editor editor) {
    Map<RangeHighlighter, HighlightInfo> map = getHighlightInfoMap(editor, false);
    if (map != null) {
      for (HighlightInfo info : map.values()) {
        if (!info.editor.equals(editor)) continue;
        if ((info.flags & HIDE_BY_ESCAPE) != 0) {
          return true;
        }
      }
    }
    return false;
  }

  private class MyAnActionListener implements AnActionListener {
    @Override
    public void beforeActionPerformed(@NotNull AnAction action, @NotNull final DataContext dataContext, @NotNull AnActionEvent event) {
      requestHideHighlights(dataContext);
    }

    @Override
    public void beforeEditorTyping(char c, @NotNull DataContext dataContext) {
      requestHideHighlights(dataContext);
    }

    private void requestHideHighlights(@NotNull DataContext dataContext) {
      final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
      if (editor == null) return;
      hideHighlights(editor, HIDE_BY_ANY_KEY);
    }
  }


  private final Key<Map<RangeHighlighter, HighlightInfo>> HIGHLIGHT_INFO_MAP_KEY = Key.create("HIGHLIGHT_INFO_MAP_KEY");

  private static class HighlightInfo {
    final Editor editor;
    @HideFlags final int flags;

    HighlightInfo(Editor editor, @HideFlags int flags) {
      this.editor = editor;
      this.flags = flags;
    }
  }
}

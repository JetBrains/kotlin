// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.highlighting;

import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.impl.ImaginaryEditor;
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

public final class HighlightManagerImpl extends HighlightManager {
  private final Project myProject;

  public HighlightManagerImpl(Project project) {
    myProject = project;
    ApplicationManager.getApplication().getMessageBus().connect(myProject).subscribe(AnActionListener.TOPIC, new MyAnActionListener());

    DocumentListener documentListener = new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent event) {
        Document document = event.getDocument();
        for (Iterator<Editor> iterator = EditorFactory.getInstance().editors(document).iterator(); iterator.hasNext(); ) {
          Editor editor = iterator.next();
          Map<RangeHighlighter, HighlightFlags> map = getHighlightInfoMap(editor, false);
          if (map == null) {
            return;
          }

          ArrayList<RangeHighlighter> highlightersToRemove = new ArrayList<>();
          for (RangeHighlighter highlighter : map.keySet()) {
            HighlightFlags info = map.get(highlighter);
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
    EditorFactory.getInstance().getEventMulticaster().addDocumentListener(documentListener, myProject);
  }

  @Nullable
  private Map<RangeHighlighter, HighlightFlags> getHighlightInfoMap(@NotNull Editor editor, boolean toCreate) {
    if (editor instanceof EditorWindow) {
      editor = ((EditorWindow)editor).getDelegate();
    }
    Map<RangeHighlighter, HighlightFlags> map = editor.getUserData(HIGHLIGHT_INFO_MAP_KEY);
    if (map == null && toCreate) {
      map = ((UserDataHolderEx)editor).putUserDataIfAbsent(HIGHLIGHT_INFO_MAP_KEY, new HashMap<>());
    }
    return map;
  }

  public RangeHighlighter @NotNull [] getHighlighters(@NotNull Editor editor) {
    Map<RangeHighlighter, HighlightFlags> highlightersMap = getHighlightInfoMap(editor, false);
    if (highlightersMap == null) return RangeHighlighter.EMPTY_ARRAY;
    Set<RangeHighlighter> set = new HashSet<>();
    for (Map.Entry<RangeHighlighter, HighlightFlags> entry : highlightersMap.entrySet()) {
      HighlightFlags info = entry.getValue();
      if (info.editor.equals(editor)) set.add(entry.getKey());
    }
    return set.toArray(RangeHighlighter.EMPTY_ARRAY);
  }

  @Override
  public boolean removeSegmentHighlighter(@NotNull Editor editor, @NotNull RangeHighlighter highlighter) {
    Map<RangeHighlighter, HighlightFlags> map = getHighlightInfoMap(editor, false);
    if (map == null) return false;
    HighlightFlags info = map.get(highlighter);
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
                                      PsiReference @NotNull [] occurrences,
                                      @NotNull TextAttributes attributes,
                                      boolean hideByTextChange,
                                      Collection<? super RangeHighlighter> outHighlighters) {
    addOccurrenceHighlights(editor, occurrences, attributes, null, hideByTextChange, outHighlighters);
  }

  @Override
  public void addOccurrenceHighlights(@NotNull Editor editor,
                                      PsiReference @NotNull [] occurrences,
                                      @NotNull TextAttributesKey attributesKey,
                                      boolean hideByTextChange,
                                      Collection<? super RangeHighlighter> outHighlighters) {
    addOccurrenceHighlights(editor, occurrences, null, attributesKey, hideByTextChange, outHighlighters);
  }

  private void addOccurrenceHighlights(@NotNull Editor editor,
                                      PsiReference @NotNull [] occurrences,
                                      @Nullable TextAttributes attributes,
                                      @Nullable TextAttributesKey attributesKey,
                                      boolean hideByTextChange,
                                      Collection<? super RangeHighlighter> outHighlighters) {
    assert attributes != null || attributesKey != null : "Both attributes and attributesKey are null";
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
        addOccurrenceHighlight(textEditor, start, end, attributes, attributesKey, flags, outHighlighters, scrollMarkColor);
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
                                     Collection<? super RangeHighlighter> outHighlighters,
                                     Color scrollMarkColor) {
    addOccurrenceHighlight(editor, start, end, attributes, null, flags, outHighlighters, scrollMarkColor);
  }

  @Override
  public void addOccurrenceHighlight(@NotNull Editor editor,
                                     int start,
                                     int end,
                                     TextAttributesKey attributesKey,
                                     int flags,
                                     Collection<? super RangeHighlighter> outHighlighters) {
    addOccurrenceHighlight(editor, start, end, null, attributesKey, flags, outHighlighters, null);
  }

  private void addOccurrenceHighlight(@NotNull Editor editor,
                                      int start,
                                      int end,
                                      @Nullable TextAttributes forcedAttributes,
                                      @Nullable TextAttributesKey attributesKey,
                                      int flags,
                                      @Nullable Collection<? super RangeHighlighter> outHighlighters,
                                      @Nullable Color scrollMarkColor) {
    MarkupModelEx markupModel = (MarkupModelEx)editor.getMarkupModel();
    markupModel.addRangeHighlighterAndChangeAttributes(attributesKey, start, end, HighlighterLayer.SELECTION - 1,
                                                       HighlighterTargetArea.EXACT_RANGE, false, highlighter -> {

        HighlightFlags info = new HighlightFlags(editor instanceof EditorWindow ? ((EditorWindow)editor).getDelegate() : editor, flags);
        Map<RangeHighlighter, HighlightFlags> map = getHighlightInfoMap(editor, true);
        map.put(highlighter, info);

        highlighter.setVisibleIfFolded(true);
        if (outHighlighters != null) {
          outHighlighters.add(highlighter);
        }

        if (forcedAttributes != null) {
          highlighter.setTextAttributes(forcedAttributes);
        }

        if (scrollMarkColor != null) {
          highlighter.setErrorStripeMarkColor(scrollMarkColor);
        }
      });
  }

  @Override
  public void addRangeHighlight(@NotNull Editor editor,
                                int startOffset,
                                int endOffset,
                                @NotNull TextAttributesKey attributesKey,
                                boolean hideByTextChange,
                                @Nullable Collection<? super RangeHighlighter> highlighters) {
    addRangeHighlight(editor, startOffset, endOffset, null, attributesKey, hideByTextChange, false, highlighters);
  }

  @Override
  public void addRangeHighlight(@NotNull Editor editor,
                                int startOffset,
                                int endOffset,
                                @NotNull TextAttributes attributes,
                                boolean hideByTextChange,
                                @Nullable Collection<? super RangeHighlighter> highlighters) {
    addRangeHighlight(editor, startOffset, endOffset, attributes, null, hideByTextChange, false, highlighters);
  }

  @Override
  public void addRangeHighlight(@NotNull Editor editor,
                                int startOffset,
                                int endOffset,
                                @NotNull TextAttributes attributes,
                                boolean hideByTextChange,
                                boolean hideByAnyKey,
                                @Nullable Collection<? super RangeHighlighter> highlighters) {
    addRangeHighlight(editor, startOffset, endOffset, attributes, null, hideByTextChange, hideByAnyKey, highlighters);
  }

  @Override
  public void addRangeHighlight(@NotNull Editor editor,
                                int startOffset,
                                int endOffset,
                                @NotNull TextAttributesKey attributesKey,
                                boolean hideByTextChange,
                                boolean hideByAnyKey,
                                @Nullable Collection<? super RangeHighlighter> highlighters) {
    addRangeHighlight(editor, startOffset, endOffset, null, attributesKey, hideByTextChange, hideByAnyKey, highlighters);
  }

  private void addRangeHighlight(@NotNull Editor editor,
                                int startOffset,
                                int endOffset,
                                @Nullable TextAttributes attributes,
                                @Nullable TextAttributesKey attributesKey,
                                boolean hideByTextChange,
                                boolean hideByAnyKey,
                                @Nullable Collection<? super RangeHighlighter> highlighters) {
    int flags = HIDE_BY_ESCAPE;
    if (hideByTextChange) {
      flags |= HIDE_BY_TEXT_CHANGE;
    }
    if (hideByAnyKey) {
      flags |= HIDE_BY_ANY_KEY;
    }

    Color scrollMarkColor = getScrollMarkColor(attributes, editor.getColorsScheme());

    addOccurrenceHighlight(editor, startOffset, endOffset, attributes, attributesKey, flags, highlighters, scrollMarkColor);
  }

  @Override
  public void addOccurrenceHighlights(@NotNull Editor editor,
                                      PsiElement @NotNull [] elements,
                                      @NotNull TextAttributes attributes,
                                      boolean hideByTextChange,
                                      Collection<? super RangeHighlighter> outHighlighters) {
    addOccurrenceHighlights(editor, elements, attributes, null, hideByTextChange, outHighlighters);
  }

  @Override
  public void addOccurrenceHighlights(@NotNull Editor editor,
                                      PsiElement @NotNull [] elements,
                                      @NotNull TextAttributesKey attributesKey,
                                      boolean hideByTextChange,
                                      Collection<? super RangeHighlighter> outHighlighters) {
    addOccurrenceHighlights(editor, elements, null, attributesKey, hideByTextChange, outHighlighters);
  }

  private void addOccurrenceHighlights(@NotNull Editor editor,
                                      PsiElement @NotNull [] elements,
                                      @Nullable TextAttributes attributes,
                                      @Nullable TextAttributesKey attributesKey,
                                      boolean hideByTextChange,
                                      Collection<? super RangeHighlighter> outHighlighters) {
    if (elements.length == 0 || editor instanceof ImaginaryEditor) return;
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
                             attributes, attributesKey, flags, outHighlighters, scrollMarkColor);
    }
  }

  private static int trimOffsetToDocumentSize(@NotNull Editor editor, int offset) {
    if (offset < 0) return 0;
    int textLength = editor.getDocument().getTextLength();
    return Math.min(offset, textLength);
  }

  @Nullable
  private static Color getScrollMarkColor(@Nullable TextAttributes attributes, @NotNull EditorColorsScheme colorScheme) {
    if (attributes == null) return null;
    if (attributes.getErrorStripeColor() != null) return attributes.getErrorStripeColor();
    if (attributes.getBackgroundColor() != null) {
      boolean isDark = ColorUtil.isDark(colorScheme.getDefaultBackground());
      return isDark ? attributes.getBackgroundColor().brighter() : attributes.getBackgroundColor().darker();
    }
    return null;
  }

  public boolean hideHighlights(@NotNull Editor editor, @HideFlags int mask) {
    Map<RangeHighlighter, HighlightFlags> map = getHighlightInfoMap(editor, false);
    if (map == null) return false;

    boolean done = false;
    List<RangeHighlighter> highlightersToRemove = new ArrayList<>();
    for (RangeHighlighter highlighter : map.keySet()) {
      HighlightFlags info = map.get(highlighter);
      if (!InjectedLanguageUtil.getTopLevelEditor(info.editor).equals(InjectedLanguageUtil.getTopLevelEditor(editor))) continue;
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
    Map<RangeHighlighter, HighlightFlags> map = getHighlightInfoMap(editor, false);
    if (map != null) {
      for (HighlightFlags info : map.values()) {
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


  private final Key<Map<RangeHighlighter, HighlightFlags>> HIGHLIGHT_INFO_MAP_KEY = Key.create("HIGHLIGHT_INFO_MAP_KEY");

  private static class HighlightFlags {
    @NotNull
    final Editor editor;
    @HideFlags final int flags;

    HighlightFlags(@NotNull Editor editor, @HideFlags int flags) {
      this.editor = editor;
      this.flags = flags;
    }
  }
}

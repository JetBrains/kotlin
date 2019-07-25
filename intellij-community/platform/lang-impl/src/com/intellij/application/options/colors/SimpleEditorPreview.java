// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options.colors;

import com.intellij.application.options.colors.highlighting.HighlightData;
import com.intellij.application.options.colors.highlighting.HighlightsExtractor;
import com.intellij.codeHighlighting.RainbowHighlighter;
import com.intellij.codeInsight.daemon.UsedColors;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptor;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.openapi.options.colors.EditorHighlightingProvidingColorSettingsPage;
import com.intellij.openapi.options.colors.RainbowColorSettingsPage;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.tree.IElementType;
import com.intellij.ui.EditorCustomization;
import com.intellij.util.Alarm;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.*;

public class SimpleEditorPreview implements PreviewPanel {
  private final ColorSettingsPage myPage;

  private final EditorEx myEditor;
  private final Alarm myBlinkingAlarm;
  private final List<HighlightData> myHighlightData = new ArrayList<>();

  private final ColorAndFontOptions myOptions;

  private final EventDispatcher<ColorAndFontSettingsListener> myDispatcher = EventDispatcher.create(ColorAndFontSettingsListener.class);
  private final HighlightsExtractor myHighlightsExtractor;

  public SimpleEditorPreview(final ColorAndFontOptions options, final ColorSettingsPage page) {
    this(options, page, true);
  }

  public SimpleEditorPreview(final ColorAndFontOptions options, final ColorSettingsPage page, final boolean navigatable) {
    myOptions = options;
    myPage = page;

    myHighlightsExtractor = new HighlightsExtractor(page.getAdditionalHighlightingTagToDescriptorMap(),
                                                    page.getAdditionalInlineElementToDescriptorMap(),
                                                    page.getAdditionalHighlightingTagToColorKeyMap());
    myEditor = (EditorEx)FontEditorPreview.createPreviewEditor(
      myHighlightsExtractor.extractHighlights(page.getDemoText(), myHighlightData), // text without tags
      10, 3, -1, myOptions.getSelectedScheme(), false);
    if (page instanceof EditorCustomization) {
      ((EditorCustomization)page).customize(myEditor);
    }

    FontEditorPreview.installTrafficLights(myEditor);
    myBlinkingAlarm = new Alarm().setActivationComponent(myEditor.getComponent());
    if (navigatable) {
      myEditor.getContentComponent().addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
          navigate(false, myEditor.xyToLogicalPosition(new Point(e.getX(), e.getY())));
        }
      });

      myEditor.getCaretModel().addCaretListener(new CaretListener() {
        @Override
        public void caretPositionChanged(@NotNull CaretEvent e) {
          navigate(true, e.getNewPosition());
        }
      });
    }
  }

  public EditorEx getEditor() {
    return myEditor;
  }

  private void navigate(boolean select, @NotNull final LogicalPosition pos) {
    int offset = myEditor.logicalPositionToOffset(pos);
    final SyntaxHighlighter highlighter = myPage.getHighlighter();

    String type;
    HighlightData highlightData = getDataFromOffset(offset);
    if (highlightData != null) {
      // tag-based navigation first
      type = RainbowHighlighter.isRainbowTempKey(highlightData.getHighlightKey())
             ? RainbowHighlighter.RAINBOW_TYPE
             : highlightData.getAdditionalColorKey() == null ? highlightData.getHighlightType()
                                                             : highlightData.getAdditionalColorKey().getExternalName();
    }
    else {
      // if failed, try the highlighter-based navigation
      type = selectItem(myEditor.getHighlighter().createIterator(offset), highlighter);
    }

    setCursor(type != null);

    if (select && type != null) {
      myDispatcher.getMulticaster().selectionInPreviewChanged(type);
    }
  }

  @Nullable
  private  HighlightData getDataFromOffset(int offset) {
    for (HighlightData highlightData : myHighlightData) {
      if (offset >= highlightData.getStartOffset() && offset <= highlightData.getEndOffset()) {
        return highlightData;
      }
    }
    return null;
  }

  @Nullable
  private static String selectItem(HighlighterIterator itr, SyntaxHighlighter highlighter) {
    IElementType tokenType = itr.getTokenType();
    if (tokenType == null) return null;

    TextAttributesKey[] highlights = highlighter.getTokenHighlights(tokenType);
    String s = null;
    for (int i = highlights.length - 1; i >= 0; i--) {
      if (highlights[i] != HighlighterColors.TEXT) {
        s = highlights[i].getExternalName();
        break;
      }
    }
    return s == null ? HighlighterColors.TEXT.getExternalName() : s;
  }

  @Override
  public JComponent getPanel() {
    return myEditor.getComponent();
  }

  @Override
  public void updateView() {
    EditorColorsScheme scheme = myOptions.getSelectedScheme();

    myEditor.setColorsScheme(scheme);

    EditorHighlighter highlighter = null;
    if (myPage instanceof EditorHighlightingProvidingColorSettingsPage) {

      highlighter = ((EditorHighlightingProvidingColorSettingsPage)myPage).createEditorHighlighter(scheme);
    }
    if (highlighter == null) {
      final SyntaxHighlighter pageHighlighter = myPage.getHighlighter();
      highlighter = HighlighterFactory.createHighlighter(pageHighlighter, scheme);
    }
    myEditor.setHighlighter(highlighter);
    updateHighlighters();

    myEditor.reinitSettings();
  }

  private void updateHighlighters() {
    UIUtil.invokeLaterIfNeeded(() -> {
      if (myEditor.isDisposed()) return;
      removeDecorations(myEditor);
      final Map<TextAttributesKey, String> displayText = ColorSettingsUtil.keyToDisplayTextMap(myPage);
      for (final HighlightData data : myHighlightData) {
        data.addHighlToView(myEditor, myOptions.getSelectedScheme(), displayText);
      }
    });
  }

  private static void removeDecorations(Editor editor) {
    editor.getMarkupModel().removeAllHighlighters();
    for (Inlay inlay : editor.getInlayModel().getInlineElementsInRange(0, editor.getDocument().getTextLength())) {
      Disposer.dispose(inlay);
    }
  }

  private static final int BLINK_COUNT = 3 * 2;

  @Override
  public void blinkSelectedHighlightType(Object description) {
    if (description instanceof EditorSchemeAttributeDescriptor) {
      String type = ((EditorSchemeAttributeDescriptor)description).getType();

      List<HighlightData> highlights = startBlinkingHighlights(myEditor,
                                                               type,
                                                               myPage.getHighlighter(), true,
                                                               myBlinkingAlarm, BLINK_COUNT, myPage);

      scrollHighlightInView(highlights);
    }
  }

  private void scrollHighlightInView(@Nullable final List<? extends HighlightData> highlightDatas) {
    if (highlightDatas == null) return;

    boolean needScroll = true;
    int minOffset = Integer.MAX_VALUE;
    for (HighlightData data : highlightDatas) {
      if (isOffsetVisible(data.getStartOffset())) {
        needScroll = false;
        break;
      }
      minOffset = Math.min(minOffset, data.getStartOffset());
    }
    if (needScroll && minOffset != Integer.MAX_VALUE) {
      LogicalPosition pos = myEditor.offsetToLogicalPosition(minOffset);
      myEditor.getScrollingModel().scrollTo(pos, ScrollType.MAKE_VISIBLE);
    }
  }

  private boolean isOffsetVisible(final int startOffset) {
    return myEditor
      .getScrollingModel()
      .getVisibleAreaOnScrollingFinished()
      .contains(myEditor.logicalPositionToXY(myEditor.offsetToLogicalPosition(startOffset)));
  }

  private void stopBlinking() {
    myBlinkingAlarm.cancelAllRequests();
  }

  private List<HighlightData> startBlinkingHighlights(final EditorEx editor,
                                                      final String attrKey,
                                                      final SyntaxHighlighter highlighter,
                                                      final boolean show,
                                                      final Alarm alarm,
                                                      final int count,
                                                      final ColorSettingsPage page) {
    if (show && count <= 0) return Collections.emptyList();
    removeDecorations(editor);
    boolean found = false;
    List<HighlightData> highlights = new ArrayList<>();
    List<HighlightData> matchingHighlights = new ArrayList<>();
    for (HighlightData highlightData : myHighlightData) {
      boolean highlight = show && (highlightData.getHighlightType().equals(attrKey) ||
                                   highlightData.getAdditionalColorKey() != null &&
                                   highlightData.getAdditionalColorKey().getExternalName().equals(attrKey));
      highlightData.addToCollection(highlights, highlight);
      if (highlight) {
        matchingHighlights.add(highlightData);
        found = true;
      }
    }
    if (show && !found && highlighter != null) {
      HighlighterIterator iterator = editor.getHighlighter().createIterator(0);
      do {
        IElementType tokenType = iterator.getTokenType();
        TextAttributesKey[] tokenHighlights = highlighter.getTokenHighlights(tokenType);
        for (final TextAttributesKey tokenHighlight : tokenHighlights) {
          String type = tokenHighlight.getExternalName();
          if (type.equals(attrKey)) {
            HighlightData highlightData = new HighlightData(iterator.getStart(), iterator.getEnd(),
                                                            CodeInsightColors.BLINKING_HIGHLIGHTS_ATTRIBUTES);
            highlights.add(highlightData);
            matchingHighlights.add(highlightData);
          }
        }
        iterator.advance();
      }
      while (!iterator.atEnd());
    }

    final Map<TextAttributesKey, String> displayText = ColorSettingsUtil.keyToDisplayTextMap(page);

    // sort highlights to avoid overlappings
    Collections.sort(highlights, Comparator.comparingInt(HighlightData::getStartOffset));
    for (int i = highlights.size() - 1; i >= 0; i--) {
      HighlightData highlightData = highlights.get(i);
      int startOffset = highlightData.getStartOffset();
      HighlightData prevHighlightData = i == 0 ? null : highlights.get(i - 1);
      if (prevHighlightData != null
          && startOffset <= prevHighlightData.getEndOffset()
          && highlightData.getHighlightType().equals(prevHighlightData.getHighlightType())) {
        prevHighlightData.setEndOffset(highlightData.getEndOffset());
      }
      else {
        highlightData.addHighlToView(editor, myOptions.getSelectedScheme(), displayText);
      }
    }
    alarm.cancelAllRequests();
    alarm.addComponentRequest(() -> startBlinkingHighlights(editor, attrKey, highlighter, !show, alarm, count - 1, page), 400);
    return matchingHighlights;
  }


  @Override
  public void addListener(@NotNull final ColorAndFontSettingsListener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public void disposeUIResources() {
    EditorFactory editorFactory = EditorFactory.getInstance();
    editorFactory.releaseEditor(myEditor);
    stopBlinking();
  }

  private void setCursor(boolean hand) {
    myEditor.setCustomCursor(SimpleEditorPreview.class, hand ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null);
  }

  void setupRainbow(@NotNull EditorColorsScheme colorsScheme, @NotNull RainbowColorSettingsPage page) {
    final List<HighlightData> initialMarkup = new ArrayList<>();
    myHighlightsExtractor.extractHighlights(page.getDemoText(), initialMarkup);
    final List<HighlightData> rainbowMarkup = setupRainbowHighlighting(
      page,
      initialMarkup,
      new RainbowHighlighter(colorsScheme).getRainbowTempKeys(),
      RainbowHighlighter.isRainbowEnabledWithInheritance(colorsScheme, page.getLanguage()));

    myHighlightData.clear();
    myHighlightData.addAll(rainbowMarkup);
  }

  @NotNull
  private List<HighlightData> setupRainbowHighlighting(@NotNull final RainbowColorSettingsPage page,
                                                       @NotNull final List<HighlightData> initialMarkup,
                                                       @NotNull final TextAttributesKey[] rainbowTempKeys,
                                                       boolean isRainbowOn) {
    int colorCount = rainbowTempKeys.length;
    if (colorCount == 0) {
      return initialMarkup;
    }
    List<HighlightData> rainbowMarkup = new ArrayList<>();

    int tempKeyIndex = 0;
    boolean repeatAnchor = true;
    for (HighlightData d : initialMarkup) {
      final TextAttributesKey highlightKey = d.getHighlightKey();
      final boolean rainbowType = page.isRainbowType(highlightKey);
      final boolean rainbowDemoType = highlightKey == RainbowHighlighter.RAINBOW_GRADIENT_DEMO;
      if (rainbowType || rainbowDemoType) {
        final HighlightData rainbowAnchor = new HighlightData(d.getStartOffset(), d.getEndOffset(), RainbowHighlighter.RAINBOW_ANCHOR);
        if (isRainbowOn) {
          // rainbow on
          HighlightData rainbowTemp;
          if (rainbowType) {
            rainbowTemp = getRainbowTemp(rainbowTempKeys, d.getStartOffset(), d.getEndOffset());
          }
          else {
            rainbowTemp = new HighlightData(d.getStartOffset(), d.getEndOffset(), rainbowTempKeys[tempKeyIndex % colorCount]);
            if (repeatAnchor && tempKeyIndex == colorCount/2) {
              // anchor [Color#3] colored twice: it the end and in the beginning of rainbow-demo string
              repeatAnchor = false;
            }
            else {
              ++tempKeyIndex;
            }
          }
          // TODO: <remove the hack>
          // At some point highlighting data is applied in reversed order. To ensure rainbow highlighting is always on top, we add it twice.
          rainbowMarkup.add(rainbowTemp);
          rainbowMarkup.add(rainbowAnchor);
          rainbowMarkup.add(d);
          rainbowMarkup.add(rainbowAnchor);
          rainbowMarkup.add(rainbowTemp);
        }
        else {
          // rainbow off
          if (rainbowType) {
            // TODO: <remove the hack>
            // See above
            rainbowMarkup.add(d);
            rainbowMarkup.add(rainbowAnchor);
            rainbowMarkup.add(d);
          }
          else {
            rainbowMarkup.add(rainbowAnchor);
          }
        }
      }
      else if (!(RainbowHighlighter.isRainbowTempKey(highlightKey) || highlightKey == RainbowHighlighter.RAINBOW_ANCHOR)) {
        // filter rainbow RAINBOW_TEMP and RAINBOW_ANCHOR
        rainbowMarkup.add(d);
      }
    }
    return rainbowMarkup;
  }

  @NotNull
  private HighlightData getRainbowTemp(@NotNull TextAttributesKey[] rainbowTempKeys,
                                       int startOffset, int endOffset) {
    String id = myEditor.getDocument().getText(TextRange.create(startOffset, endOffset));
    int index = UsedColors.getOrAddColorIndex((EditorImpl)myEditor, id, rainbowTempKeys.length);
    return new HighlightData(startOffset, endOffset, rainbowTempKeys[index]);
  }
}

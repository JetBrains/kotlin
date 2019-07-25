// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options.colors.highlighting;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collection;
import java.util.Map;

import static com.intellij.openapi.editor.colors.CodeInsightColors.BLINKING_HIGHLIGHTS_ATTRIBUTES;

public class HighlightData {
  private final int myStartOffset;
  private int myEndOffset;
  private final TextAttributesKey myHighlightType;
  private final ColorKey myAdditionalColorKey;

  public HighlightData(int startOffset, TextAttributesKey highlightType, @Nullable ColorKey additionalColorKey) {
    myStartOffset = startOffset;
    myHighlightType = highlightType;
    myAdditionalColorKey = additionalColorKey;
  }

  public HighlightData(int startOffset, int endOffset, TextAttributesKey highlightType) {
    this(startOffset, endOffset, highlightType, null);
  }

  public HighlightData(int startOffset, int endOffset, TextAttributesKey highlightType, @Nullable ColorKey additionalColorKey) {
    myStartOffset = startOffset;
    myEndOffset = endOffset;
    myHighlightType = highlightType;
    myAdditionalColorKey = additionalColorKey;
  }

  public void addToCollection(@NotNull Collection<? super HighlightData> list, boolean highlighted) {
    list.add(this);
    if (highlighted) list.add(new HighlightData(getStartOffset(), getEndOffset(), BLINKING_HIGHLIGHTS_ATTRIBUTES, getAdditionalColorKey()));
  }

  public void addHighlToView(final Editor view, EditorColorsScheme scheme, final Map<TextAttributesKey,String> displayText) {

    // XXX: Hack
    if (HighlighterColors.BAD_CHARACTER.equals(myHighlightType)) {
      return;
    }

    final TextAttributes attr = scheme.getAttributes(myHighlightType);
    if (attr != null) {
      UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
        try {
          // IDEA-53203: add ERASE_MARKER for manually defined attributes
          view.getMarkupModel().addRangeHighlighter(myStartOffset, myEndOffset, HighlighterLayer.ADDITIONAL_SYNTAX,
                                                    TextAttributes.ERASE_MARKER, HighlighterTargetArea.EXACT_RANGE);
          RangeHighlighter highlighter = view.getMarkupModel()
            .addRangeHighlighter(myStartOffset, myEndOffset, HighlighterLayer.ADDITIONAL_SYNTAX, attr,
                                 HighlighterTargetArea.EXACT_RANGE);
          final Color errorStripeColor = attr.getErrorStripeColor();
          highlighter.setErrorStripeMarkColor(errorStripeColor);
          final String tooltip = displayText.get(myHighlightType);
          highlighter.setErrorStripeTooltip(tooltip);
          if (highlighter instanceof RangeHighlighterEx) ((RangeHighlighterEx)highlighter).setVisibleIfFolded(true);
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  public int getStartOffset() {
    return myStartOffset;
  }

  public int getEndOffset() {
    return myEndOffset;
  }

  public void setEndOffset(int endOffset) {
    myEndOffset = endOffset;
  }

  public String getHighlightType() {
    return myHighlightType.getExternalName();
  }

  public TextAttributesKey getHighlightKey() {
    return myHighlightType;
  }
  
  public ColorKey getAdditionalColorKey() {
    return myAdditionalColorKey;
  }
}

// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.colors.highlighting;

import com.intellij.codeInsight.daemon.impl.HintRenderer;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collection;
import java.util.Map;

import static com.intellij.openapi.editor.colors.CodeInsightColors.BLINKING_HIGHLIGHTS_ATTRIBUTES;

public class InlineElementData extends HighlightData {
  private final String myText;
  private final boolean myAddBorder;

  public InlineElementData(int offset, TextAttributesKey attributesKey, String text, ColorKey additionalColorKey) {
    this(offset, attributesKey, text, false, additionalColorKey);
  }

  private InlineElementData(int offset, TextAttributesKey attributesKey, String text, boolean highlighted, ColorKey additionalColorKey) {
    super(offset, offset, attributesKey, additionalColorKey);
    myText = text;
    myAddBorder = highlighted;
  }

  public String getText() {
    return myText;
  }

  @Override
  public void addHighlToView(Editor view, EditorColorsScheme scheme, Map<TextAttributesKey, String> displayText) {
    int offset = getStartOffset();
    RendererWrapper renderer = new RendererWrapper(new HintRenderer(myText) {
      @Nullable
      @Override
      protected TextAttributes getTextAttributes(@NotNull Editor editor) {
        return editor.getColorsScheme().getAttributes(getHighlightKey());
      }
    });
    renderer.drawBorder = myAddBorder;
    view.getInlayModel().addInlineElement(offset, false, renderer);
  }

  @Override
  public void addToCollection(@NotNull Collection<? super HighlightData> list, boolean highlighted) {
    list.add(new InlineElementData(getStartOffset(), getHighlightKey(), myText, highlighted, getAdditionalColorKey()));
  }

  public static class RendererWrapper implements EditorCustomElementRenderer {
    private final EditorCustomElementRenderer myDelegate;
    private boolean drawBorder;

    public RendererWrapper(EditorCustomElementRenderer delegate) {
      myDelegate = delegate;
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
      return myDelegate.calcWidthInPixels(inlay);
    }

    @Override
    public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle r, @NotNull TextAttributes textAttributes) {
      myDelegate.paint(inlay, g, r, textAttributes);
      if (drawBorder) {
        TextAttributes attributes = inlay.getEditor().getColorsScheme().getAttributes(BLINKING_HIGHLIGHTS_ATTRIBUTES);
        if (attributes != null && attributes.getEffectColor() != null) {
          g.setColor(attributes.getEffectColor());
          g.drawRect(r.x, r.y, r.width, r.height);
        }
      }
    }
  }

}

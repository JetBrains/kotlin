// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.richcopy;

import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.richcopy.model.SyntaxInfo;
import com.intellij.openapi.editor.richcopy.view.HtmlSyntaxInfoReader;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public final class HtmlSyntaxInfoUtil {
  
  @Nullable
  public static CharSequence getHtmlContent(@NotNull PsiFile file,
                                            @NotNull CharSequence text,
                                            @Nullable SyntaxInfoBuilder.RangeIterator ownRangeIterator,
                                            @NotNull EditorColorsScheme schemeToUse,
                                            int startOffset,
                                            int endOffset) {
    EditorHighlighter highlighter =
      HighlighterFactory.createHighlighter(file.getViewProvider().getVirtualFile(), schemeToUse, file.getProject());
    highlighter.setText(text);

    SyntaxInfoBuilder.HighlighterRangeIterator highlighterRangeIterator =
      new SyntaxInfoBuilder.HighlighterRangeIterator(highlighter, startOffset, endOffset);
    ownRangeIterator = ownRangeIterator == null
                       ? highlighterRangeIterator
                       : new SyntaxInfoBuilder.CompositeRangeIterator(schemeToUse, highlighterRangeIterator, ownRangeIterator);

    return getHtmlContent(text, ownRangeIterator, schemeToUse, endOffset);
  }

  @Nullable
  public static CharSequence getHtmlContent(@NotNull CharSequence text,
                                            @NotNull SyntaxInfoBuilder.RangeIterator ownRangeIterator,
                                            @NotNull EditorColorsScheme schemeToUse,
                                            int stopOffset) {
    SyntaxInfoBuilder.Context context = new SyntaxInfoBuilder.Context(text, schemeToUse, 0);
    SyntaxInfoBuilder.MyMarkupIterator iterator = new SyntaxInfoBuilder.MyMarkupIterator(text, ownRangeIterator, schemeToUse);

    try {
      context.iterate(iterator, stopOffset);
    }
    finally {
      iterator.dispose();
    }
    SyntaxInfo info = context.finish();
    try (HtmlSyntaxInfoReader data = new SimpleHtmlSyntaxInfoReader(info)) {
      data.setRawText(text.toString());
      return data.getBuffer();
    }
    catch (IOException e) {
      Logger.getInstance(HtmlSyntaxInfoUtil.class).error(e);
    }
    return null;
  }

  private final static class SimpleHtmlSyntaxInfoReader extends HtmlSyntaxInfoReader {
    
    public SimpleHtmlSyntaxInfoReader(SyntaxInfo info) {
      super(info, 2);
    }

    @Override
    protected void appendCloseTags() {
     
    }

    @Override
    protected void appendStartTags() {
      
    }

    @Override
    protected void defineBackground(int id, @NotNull StringBuilder styleBuffer) {

    }

    @Override
    protected void appendFontFamilyRule(@NotNull StringBuilder styleBuffer, int fontFamilyId) {

    }
  }
}

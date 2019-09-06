// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.codeStyle;

import com.intellij.formatting.FormatTextRanges;
import com.intellij.formatting.FormattingRangesExtender;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class FormattingRangesExtenderImpl implements FormattingRangesExtender {
  private final Document myDocument;

  FormattingRangesExtenderImpl(@NotNull Document document) {
    myDocument = document;
  }

  @Override
  public List<TextRange> getExtendedRanges(@NotNull FormatTextRanges ranges) {
    return ContainerUtil.map(ranges.getTextRanges(), (range) -> {
      int startOffset = Math.max(range.getStartOffset() - 500, 0);
      int endOffset = Math.min(range.getEndOffset() + 500, myDocument.getTextLength());
      return new TextRange(startOffset, endOffset);
    });
  }
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.codeStyle;

import com.intellij.formatting.FormatTextRanges;
import com.intellij.formatting.FormattingRangesExtender;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UnfairTextRange;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class FormattingRangesExtenderImpl implements FormattingRangesExtender {

  @Override
  public List<TextRange> getExtendedRanges(@NotNull FormatTextRanges ranges) {
    return ContainerUtil.map(ranges.getTextRanges(), (range) -> {
      return new UnfairTextRange(range.getStartOffset() - 500, range.getEndOffset() + 500);
    });
  }
}

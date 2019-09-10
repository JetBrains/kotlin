// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface FormattingRangesExtender {

  List<TextRange> getExtendedRanges(@NotNull List<TextRange> ranges);
}

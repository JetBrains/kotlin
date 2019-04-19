// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting.templateLanguages;

import com.intellij.formatting.*;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * A block that's created when template & template data language blocks overlap in an irreconcilable way. The block covers the entire overlap
 * area and isn't reformatted.
 *
 * @author peter
 */
class ErrorLeafBlock implements Block {
  private final int myStartOffset;
  private final int myEndOffset;

  ErrorLeafBlock(int startOffset, int endOffset) {
    myStartOffset = startOffset;
    myEndOffset = endOffset;
  }

  @NotNull
  @Override
  public TextRange getTextRange() {
    return TextRange.create(myStartOffset, myEndOffset);
  }

  @NotNull
  @Override
  public List<Block> getSubBlocks() {
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public Wrap getWrap() {
    return null;
  }

  @Nullable
  @Override
  public Indent getIndent() {
    return null;
  }

  @Nullable
  @Override
  public Alignment getAlignment() {
    return null;
  }

  @Nullable
  @Override
  public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
    return null;
  }

  @NotNull
  @Override
  public ChildAttributes getChildAttributes(int newChildIndex) {
    return new ChildAttributes(null, null);
  }

  @Override
  public boolean isIncomplete() {
    return true;
  }

  @Override
  public boolean isLeaf() {
    return true;
  }
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting;

import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class InitialInfoBuilderState {
  public final Block parentBlock;
  public final WrapImpl parentBlockWrap;
  public final CompositeBlockWrapper wrappedBlock;

  public Block previousBlock;

  private final List<AbstractBlockWrapper> myWrappedChildren = new ArrayList<>();

  InitialInfoBuilderState(@NotNull Block parentBlock,
                          @NotNull CompositeBlockWrapper wrappedBlock,
                          @Nullable WrapImpl parentBlockWrap) {
    this.parentBlock = parentBlock;
    this.wrappedBlock = wrappedBlock;
    this.parentBlockWrap = parentBlockWrap;
  }

  public int getIndexOfChildBlockToProcess() {
    return myWrappedChildren.size();
  }

  public boolean childBlockProcessed(@NotNull Block child,
                                     @NotNull AbstractBlockWrapper wrappedChild,
                                     CommonCodeStyleSettings.IndentOptions options) {
    myWrappedChildren.add(wrappedChild);
    previousBlock = child;

    int subBlocksNumber = parentBlock.getSubBlocks().size();
    if (myWrappedChildren.size() > subBlocksNumber) {
      return true;
    }
    else if (myWrappedChildren.size() == subBlocksNumber) {
      setDefaultIndents(myWrappedChildren, options.USE_RELATIVE_INDENTS);
      wrappedBlock.setChildren(myWrappedChildren);
      return true;
    }
    return false;
  }

  public boolean isProcessed() {
    return myWrappedChildren.size() == parentBlock.getSubBlocks().size();
  }

  private static void setDefaultIndents(final List<AbstractBlockWrapper> list, boolean useRelativeIndents) {
    for (AbstractBlockWrapper wrapper : list) {
      if (wrapper.getIndent() == null) {
        wrapper.setIndent((IndentImpl)Indent.getContinuationWithoutFirstIndent(useRelativeIndents));
      }
    }
  }

}
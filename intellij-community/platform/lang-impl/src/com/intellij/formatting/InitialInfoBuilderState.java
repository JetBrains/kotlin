/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.formatting;

import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class InitialInfoBuilderState {
  public final Block parentBlock;
  public final WrapImpl parentBlockWrap;
  public final CompositeBlockWrapper wrappedBlock;
  public final boolean readOnly;

  public Block previousBlock;

  private final List<AbstractBlockWrapper> myWrappedChildren = ContainerUtil.newArrayList();

  InitialInfoBuilderState(@NotNull Block parentBlock,
                          @NotNull CompositeBlockWrapper wrappedBlock,
                          @Nullable WrapImpl parentBlockWrap,
                          boolean readOnly) {
    this.parentBlock = parentBlock;
    this.wrappedBlock = wrappedBlock;
    this.parentBlockWrap = parentBlockWrap;
    this.readOnly = readOnly;
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
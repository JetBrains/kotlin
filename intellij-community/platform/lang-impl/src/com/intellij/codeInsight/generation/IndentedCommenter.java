// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.generation;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

public interface IndentedCommenter extends Commenter {
  /**
   * Used to override CodeStyleSettings#LINE_COMMENT_AT_FIRST_COLUMN option
   * @return true or false to override, null to use settings option
   */
  @Nullable
  Boolean forceIndentedLineComment();

  /**
   * Used to override CodeStyleSettings#BLOCK_COMMENT_AT_FIRST_COLUMN option
   * @return true or false to override, null to use settings option
   */
  @Nullable
  default Boolean forceIndentedBlockComment() {
    return forceIndentedLineComment();
  }
}

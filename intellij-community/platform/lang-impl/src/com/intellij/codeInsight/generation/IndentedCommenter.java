/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.codeInsight.generation;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

/**
 * @author oleg
 */
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

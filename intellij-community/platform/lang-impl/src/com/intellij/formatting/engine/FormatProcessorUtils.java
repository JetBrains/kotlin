/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.formatting.engine;

import com.intellij.formatting.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;

class FormatProcessorUtils {
  
  
  private static int calcShift(@NotNull final IndentInside oldIndent,
                               @NotNull final IndentInside newIndent,
                               @NotNull final CommonCodeStyleSettings.IndentOptions options)
  {
    if (oldIndent.equals(newIndent)) return 0;
    return newIndent.getSpacesCount(options) - oldIndent.getSpacesCount(options);
  }
  
  static int replaceWhiteSpace(final FormattingModel model,
                               @NotNull final LeafBlockWrapper block,
                               int shift,
                               final CharSequence _newWhiteSpace,
                               final CommonCodeStyleSettings.IndentOptions options
  ) {
    final WhiteSpace whiteSpace = block.getWhiteSpace();
    final TextRange textRange = whiteSpace.getTextRange();
    final TextRange wsRange = textRange.shiftRight(shift);
    final String newWhiteSpace = _newWhiteSpace.toString();
    TextRange newWhiteSpaceRange = model instanceof FormattingModelEx
                                   ? ((FormattingModelEx) model).replaceWhiteSpace(wsRange, block.getNode(), newWhiteSpace)
                                   : model.replaceWhiteSpace(wsRange, newWhiteSpace);

    shift += newWhiteSpaceRange.getLength() - textRange.getLength();

    if (block.isLeaf() && whiteSpace.containsLineFeeds() && block.containsLineFeeds()) {
      final TextRange currentBlockRange = block.getTextRange().shiftRight(shift);

      IndentInside oldBlockIndent = whiteSpace.getInitialLastLineIndent();
      IndentInside whiteSpaceIndent = IndentInside.createIndentOn(IndentInside.getLastLine(newWhiteSpace));
      final int shiftInside = calcShift(oldBlockIndent, whiteSpaceIndent, options);

      if (shiftInside != 0 || !oldBlockIndent.equals(whiteSpaceIndent)) {
        final TextRange newBlockRange = model.shiftIndentInsideRange(block.getNode(), currentBlockRange, shiftInside);
        shift += newBlockRange.getLength() - block.getLength();
      }
    }
    return shift;
  }
  
}

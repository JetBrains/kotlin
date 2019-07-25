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
package com.intellij.psi.codeStyle.autodetect;

import com.intellij.formatting.ASTBlock;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.formatter.common.NewLineBlocksIterator;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FormatterBasedLineIndentInfoBuilder {
  private static final int MAX_NEW_LINE_BLOCKS_TO_PROCESS = 500;

  private final ProgressIndicator myProgressIndicator;
  private final Document myDocument;
  private final CharSequence myText;
  private final Block myRootBlock;

  public FormatterBasedLineIndentInfoBuilder(@NotNull Document document, 
                                             @NotNull Block rootBlock, 
                                             @Nullable ProgressIndicator indicator) 
  {
    myDocument = document;
    myText = myDocument.getCharsSequence();
    myRootBlock = rootBlock;
    myProgressIndicator = indicator;
  }

  public List<LineIndentInfo> build() {
    List<Block> newLineBlocks = getBlocksStartingNewLine();
    
    return ContainerUtil.map(newLineBlocks, newLineBlock -> {
      int blockStartOffset = newLineBlock.getTextRange().getStartOffset();
      int line = myDocument.getLineNumber(blockStartOffset);
      int lineStartOffset = myDocument.getLineStartOffset(line);

      if (rangeHasTabs(lineStartOffset, blockStartOffset)) {
        return LineIndentInfo.LINE_WITH_TABS;
      }

      if (hasNormalIndent(newLineBlock)) {
        return LineIndentInfo.newNormalIndent(blockStartOffset - lineStartOffset);
      }
      else {
        return LineIndentInfo.LINE_WITH_NOT_COUNTABLE_INDENT;
      }
    });
  }
  
  private static boolean hasNormalIndent(Block block) {
    final TextRange range = block.getTextRange();
    final int startOffset = range.getStartOffset();
  
    List<Indent.Type> allIndents = getIndentOnStartOffset(block, range, startOffset);

    if (hasOnlyNormalOrNoneIndents(allIndents)) {
      int normalIndents = ContainerUtil.filter(allIndents, type -> type == Indent.Type.NORMAL).size();
      return normalIndents < 2;
    }
    
    return false;
  }

  private static boolean hasOnlyNormalOrNoneIndents(List<Indent.Type> indents) {
    Indent.Type outerMostIndent = indents.get(0);
    if (outerMostIndent != Indent.Type.NONE && outerMostIndent != Indent.Type.NORMAL) {
      return false;
    }

    List<Indent.Type> innerIndents = indents.subList(1, indents.size());
    for (Indent.Type indent : innerIndents) {
      if (indent != Indent.Type.NONE && indent != Indent.Type.NORMAL 
          && indent != Indent.Type.CONTINUATION_WITHOUT_FIRST) {
        //continuation without first here because it is CONTINUATION only if it's owner is not the first child
        return false;
      }
    }

    return true;
  }

  private static List<Indent.Type> getIndentOnStartOffset(Block block, TextRange range, int startOffset) {
    List<Indent.Type> indentsOnStartOffset = new ArrayList<>();
    
    while (block != null && range.getStartOffset() == startOffset) {
      Indent.Type type = block.getIndent() != null ? block.getIndent().getType() : Indent.Type.CONTINUATION_WITHOUT_FIRST;
      indentsOnStartOffset.add(type);
      
      if (block instanceof AbstractBlock) {
        ((AbstractBlock)block).setBuildIndentsOnly(true);
      }
      List<Block> subBlocks = block.getSubBlocks();
      block = subBlocks.isEmpty() ? null : subBlocks.get(0);
    }
    
    return indentsOnStartOffset;
  }

  @NotNull
  private List<Block> getBlocksStartingNewLine() {
    NewLineBlocksIterator newLineBlocksIterator = new NewLineBlocksIterator(myRootBlock, myDocument, myProgressIndicator);

    List<Block> newLineBlocks = new ArrayList<>();
    int currentLine = 0;
    while (newLineBlocksIterator.hasNext() && currentLine < MAX_NEW_LINE_BLOCKS_TO_PROCESS) {
      Block next = newLineBlocksIterator.next();
      if (next instanceof ASTBlock && ((ASTBlock)next).getNode() instanceof PsiComment) {
        continue;
      }
      newLineBlocks.add(next);
      currentLine++;
    }

    return newLineBlocks;
  }
  
  private boolean rangeHasTabs(int lineStartOffset, int textStartOffset) {
    return CharArrayUtil.indexOf(myText, "\t", lineStartOffset, textStartOffset) > 0;
  }
}

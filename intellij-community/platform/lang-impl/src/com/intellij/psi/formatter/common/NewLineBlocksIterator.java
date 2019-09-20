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
package com.intellij.psi.formatter.common;

import com.intellij.formatting.Block;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

public class NewLineBlocksIterator implements Iterator<Block> {
  private final ProgressIndicator myIndicator;
  private final Document myDocument;
  private final int myTotalLines;

  private int myCurrentLineStartOffset;
  private int myCurrentDocumentLine;
  private final Stack<Block> myStack = new Stack<>();

  @TestOnly
  public NewLineBlocksIterator(@NotNull Block root, @NotNull Document document) {
    this(root, document, null);
  }
  
  public NewLineBlocksIterator(@NotNull Block root, @NotNull Document document, @Nullable ProgressIndicator indicator) {
    myStack.add(root);
    myDocument = document;
    myTotalLines = myDocument.getLineCount();

    myCurrentDocumentLine = 0;
    myCurrentLineStartOffset = 0;

    myIndicator = indicator;
  }

  @Override
  public boolean hasNext() {
    if (myCurrentDocumentLine < myTotalLines) {
      popUntilTopBlockStartsNewLine();
      return !myStack.isEmpty();
    }
    return false;
  }

  private void popUntilTopBlockStartsNewLine() {
    popUntilTopBlockStartOffsetGreaterOrEqual(myCurrentLineStartOffset);
    if (myStack.isEmpty()) return;

    Block block = myStack.peek();
    while (block != null && !isStartingNewLine(block)) {
      checkCancelled();
      myCurrentDocumentLine++;
      if (myCurrentDocumentLine >= myTotalLines) {
        myStack.clear();
        break;
      }
      myCurrentLineStartOffset = myDocument.getLineStartOffset(myCurrentDocumentLine);
      popUntilTopBlockStartOffsetGreaterOrEqual(myCurrentLineStartOffset);
      block = myStack.isEmpty() ? null : myStack.peek();
    }
  }

  private void checkCancelled() {
    if (myIndicator != null) {
      myIndicator.checkCanceled();
    }
  }

  @Override
  public Block next() {
    popUntilTopBlockStartsNewLine();

    Block current = myStack.peek();
    TextRange currentBlockRange = current.getTextRange();

    myCurrentDocumentLine = myDocument.getLineNumber(currentBlockRange.getStartOffset());
    myCurrentDocumentLine++;
    if (myCurrentDocumentLine < myTotalLines) {
      myCurrentLineStartOffset = myDocument.getLineStartOffset(myCurrentDocumentLine);
      if (currentBlockRange.getEndOffset() < myCurrentLineStartOffset) {
        myStack.pop();
      }
      else {
        pushAll(current);
      }
    }

    return current;
  }

  private void popUntilTopBlockStartOffsetGreaterOrEqual(final int lineStartOffset) {
    while (!myStack.isEmpty()) {
      checkCancelled();
      Block current = myStack.peek();
      TextRange range = current.getTextRange();
      if (range.getStartOffset() < lineStartOffset) {
        myStack.pop();
        if (range.getEndOffset() > lineStartOffset) {
          pushAll(current);
        }
      }
      else {
        break;
      }
    }
  }

  private void pushAll(Block current) {
    if (current instanceof AbstractBlock) {
      //building blocks as fast as possible
      ((AbstractBlock)current).setBuildIndentsOnly(true);
    }

    List<Block> blocks = current.getSubBlocks();
    ListIterator<Block> iterator = blocks.listIterator(blocks.size());
    while (iterator.hasPrevious()) {
      myStack.push(iterator.previous());
    }
  }

  private boolean isStartingNewLine(Block block) {
    TextRange range = block.getTextRange();
    int blockStart = range.getStartOffset();

    int lineNumber = myDocument.getLineNumber(blockStart);
    int lineStartOffset = myDocument.getLineStartOffset(lineNumber);

    CharSequence text = myDocument.getCharsSequence();
    return CharArrayUtil.isEmptyOrSpaces(text, lineStartOffset, blockStart);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}

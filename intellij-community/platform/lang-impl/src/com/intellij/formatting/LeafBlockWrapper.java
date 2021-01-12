/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LeafBlockWrapper extends AbstractBlockWrapper {
  private static final int CONTAIN_LINE_FEEDS = 4;
  private static final int READ_ONLY = 8;
  private static final int LEAF = 16;

  private final int mySymbolsAtTheLastLine;
  private LeafBlockWrapper myPreviousBlock;
  private LeafBlockWrapper myNextBlock;
  private SpacingImpl mySpaceProperty;

  /**
   * Shortcut for calling
   * {@link #LeafBlockWrapper(Block, CompositeBlockWrapper, WhiteSpace, FormattingDocumentModel, CommonCodeStyleSettings.IndentOptions, LeafBlockWrapper, boolean, TextRange)}
   * with {@link Block#getTextRange() text range associated with the given block}.
   *
   * @param block               block to wrap
   * @param parent              wrapped parent block
   * @param whiteSpaceBefore    white space before the target block to wrap
   * @param model               formatting model to use during current wrapper initialization
   * @param options             code formatting options
   * @param previousTokenBlock  previous token block
   * @param isReadOnly          flag that indicates if target block is read-only
   */
  public LeafBlockWrapper(final Block block,
                          @Nullable CompositeBlockWrapper parent,
                          @NotNull WhiteSpace whiteSpaceBefore,
                          FormattingDocumentModel model,
                          CommonCodeStyleSettings.IndentOptions options,
                          LeafBlockWrapper previousTokenBlock,
                          boolean isReadOnly)
  {
    this(block, parent, whiteSpaceBefore, model, options, previousTokenBlock, isReadOnly, block.getTextRange());
  }

  LeafBlockWrapper(Block block,
                   CompositeBlockWrapper parent,
                   @NotNull WhiteSpace whiteSpaceBefore,
                   FormattingDocumentModel model,
                   CommonCodeStyleSettings.IndentOptions options,
                   LeafBlockWrapper previousTokenBlock,
                   boolean isReadOnly,
                   final TextRange textRange)
  {
    super(block, whiteSpaceBefore, parent, textRange);
    myPreviousBlock = previousTokenBlock;
    final int lastLineNumber = model.getLineNumber(textRange.getEndOffset());

    int flagsValue = myFlags;
    final boolean containsLineFeeds = model.getLineNumber(textRange.getStartOffset()) != lastLineNumber;
    flagsValue |= containsLineFeeds ? CONTAIN_LINE_FEEDS:0;

    // We need to perform such a complex calculation because block construction algorithm is allowed to create 'leaf' blocks
    // that contain more than one token interleaved white space that contains either tabulations or line breaks.
    // E.g. consider the following code:
    //
    // public
    //  void foo() {}
    //
    // 'public void' here is a single 'leaf' token and it's second part 'void' is preceeded by tabulaton symbol. Hence, we need
    // correctly calculate number of symbols occupied by the current token at last line.
    int start = containsLineFeeds ? model.getLineStartOffset(lastLineNumber) : textRange.getStartOffset();
    int symbols = 0;
    CharSequence text = model.getDocument().getCharsSequence();
    for (int i = start; i < textRange.getEndOffset(); i++) {
      if (text.charAt(i) == '\t') {
        symbols += options.TAB_SIZE;
      } else {
        symbols++;
      }
    }
    mySymbolsAtTheLastLine = symbols;
    flagsValue |= isReadOnly ? READ_ONLY:0;
    final boolean isLeaf = block.isLeaf();
    flagsValue |= isLeaf ? LEAF : 0;

    myFlags = flagsValue;
  }

  public final boolean containsLineFeeds() {
    return (myFlags & CONTAIN_LINE_FEEDS) != 0;
  }

  public int getSymbolsAtTheLastLine() {
    return mySymbolsAtTheLastLine;
  }

  @Override
  public LeafBlockWrapper getPreviousBlock() {
    return myPreviousBlock;
  }

  public LeafBlockWrapper getNextBlock() {
    return myNextBlock;
  }

  public void setNextBlock(final LeafBlockWrapper nextBlock) {
    myNextBlock = nextBlock;
  }

  @Override
  protected boolean indentAlreadyUsedBefore(final AbstractBlockWrapper child) {
    return false;
  }

  @Override
  public IndentData getNumberOfSymbolsBeforeBlock() {
    int spaces = getWhiteSpace().getSpaces();
    int indentSpaces = getWhiteSpace().getIndentSpaces();

    if (getWhiteSpace().containsLineFeeds()) {
      return new IndentData(indentSpaces, spaces);
    }

    for (LeafBlockWrapper current = this.getPreviousBlock(); current != null; current = current.getPreviousBlock()) {
      spaces += current.getWhiteSpace().getSpaces();
      spaces += current.getSymbolsAtTheLastLine();
      indentSpaces += current.getWhiteSpace().getIndentSpaces();
      if (current.getWhiteSpace().containsLineFeeds()) {
        break;
      }
    }
    return new IndentData(indentSpaces, spaces);
  }

  @Override
  public void dispose() {
    super.dispose();
    myPreviousBlock = null;
    myNextBlock = null;
    mySpaceProperty = null;
  }

  /**
   * @return    spacing between current block and its left sibling
   */
  public SpacingImpl getSpaceProperty() {
    return mySpaceProperty;
  }

  public IndentData calculateOffset(final CommonCodeStyleSettings.IndentOptions options) {
    // Calculate result as an indent of current block from parent plus parent block indent.
    if (myIndentFromParent != null) {
      final AbstractBlockWrapper firstIndentedParent = findFirstIndentedParent();
      final IndentData indentData = new IndentData(myIndentFromParent.getIndentSpaces(), myIndentFromParent.getSpaces());
      if (firstIndentedParent == null) {
        return indentData;
      } else {
        final WhiteSpace whiteSpace = firstIndentedParent.getWhiteSpace();
        return new IndentData(whiteSpace.getIndentOffset(), whiteSpace.getSpaces()).add(indentData);
      }
    }

    // Consider that current block is not indented if it doesn't have a parent block.
    if (myParent == null) return new IndentData(0);

    // Define that current block and all its parents that start at the same offset can't use first child indent as block indent.
    if (getIndent().isAbsolute()) {
      setCanUseFirstChildIndentAsBlockIndent(false);
      AbstractBlockWrapper current = this;
      while (current != null && current.getStartOffset() == getStartOffset()) {
        current.setCanUseFirstChildIndentAsBlockIndent(false);
        current = current.myParent;
      }
    }

    return myParent.getChildOffset(this, options, this.getStartOffset());
  }

  public void setSpaceProperty(@Nullable final SpacingImpl currentSpaceProperty) {
    mySpaceProperty = currentSpaceProperty;
  }

  public final boolean isLeaf() {
    return (myFlags & LEAF) != 0;
  }

  public boolean contains(final int offset) {
    return myStart < offset && myEnd > offset;
  }

  public TextRange getTextRange() {
    return new TextRange(myStart, myEnd);
  }

  public boolean isEndOfCodeBlock() {
    ASTNode node = getNode();
    return node != null && node.getTextLength() == 1 && node.getChars().charAt(0) == '}';
  }
}

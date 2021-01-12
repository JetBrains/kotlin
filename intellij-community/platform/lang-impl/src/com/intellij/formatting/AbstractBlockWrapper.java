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
import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author lesya
 */
public abstract class AbstractBlockWrapper {

  private static final Set<IndentImpl.Type> RELATIVE_INDENT_TYPES =
    ContainerUtil.set(Indent.Type.NORMAL, Indent.Type.CONTINUATION, Indent.Type.CONTINUATION_WITHOUT_FIRST);

  private @NotNull final WhiteSpace myWhiteSpaceBefore;

  protected CompositeBlockWrapper myParent;
  protected int                   myStart;
  protected int                   myEnd;
  protected int                   myFlags;

  static int CAN_USE_FIRST_CHILD_INDENT_AS_BLOCK_INDENT = 1;
  static int INCOMPLETE                                 = 2;

  private final Language myLanguage;

  protected IndentInfo myIndentFromParent = null;
  private   IndentImpl myIndent           = null;
  private AlignmentImpl myAlignment;
  private WrapImpl      myWrap;
  private final ASTNode myNode;

  public AbstractBlockWrapper(final Block block,
                              final @NotNull WhiteSpace whiteSpaceBefore,
                              final CompositeBlockWrapper parent,
                              final TextRange textRange) {
    myWhiteSpaceBefore = whiteSpaceBefore;
    myParent = parent;
    myStart = textRange.getStartOffset();
    myEnd = textRange.getEndOffset();

    myFlags = CAN_USE_FIRST_CHILD_INDENT_AS_BLOCK_INDENT | (block.isIncomplete() ? INCOMPLETE : 0);
    myAlignment = (AlignmentImpl)block.getAlignment();
    myWrap = (WrapImpl)block.getWrap();
    myLanguage = deriveLanguage(block);
    myNode = block instanceof ASTBlock ? ((ASTBlock) block).getNode() : null;
  }

  @Nullable
  private static Language deriveLanguage(@NotNull Block block) {
    if (block instanceof BlockEx) {
      return ((BlockEx)block).getLanguage();
    }
    return null;
  }

  /**
   * Returns the whitespace preceding the block.
   *
   * @return the whitespace preceding the block
   */
  @NotNull
  public WhiteSpace getWhiteSpace() {
    return myWhiteSpaceBefore;
  }

  /**
   * Returns the AST node corresponding to the block, if known.
   *
   * @return the AST node or null
   */
  @Nullable
  public ASTNode getNode() {
    return myNode;
  }

  /**
   * Returns the list of wraps for this block and its parent blocks that start at the same offset. The returned list is ordered from top
   * to bottom (higher-level wraps go first). Stops if the wrap for a block is marked as "ignore parent wraps".
   *
   * @return the list of wraps.
   */
  public ArrayList<WrapImpl> getWraps() {
    final ArrayList<WrapImpl> result = new ArrayList<>(3);
    AbstractBlockWrapper current = this;
    while(current != null && current.getStartOffset() == getStartOffset()) {
      final WrapImpl wrap = current.getOwnWrap();
      if (wrap != null && !result.contains(wrap)) result.add(0, wrap);
      if (wrap != null && wrap.getIgnoreParentWraps()) break;
      current = current.myParent;
    }
    return result;
  }

  public int getStartOffset() {
    return myStart;
  }

  public int getEndOffset() {
    return myEnd;
  }

  public int getLength() {
    return myEnd - myStart;
  }

  /**
   * There is a possible case that particular block's language differs from the language implied by the file type. We need to
   * distinguish such a situation because, for example in case of indent calculation (code style settings for different languages
   * may have different indent values).
   * <p/>
   * This method allows to retrieve the language associated with the current block (if provided).
   * 
   * @return current block's language (if provided)
   */
  @Nullable
  public Language getLanguage() {
    return myLanguage;
  }

  /**
   * Applies given start offset to the current block wrapper and recursively calls this method on parent block wrapper
   * if it starts at the same place as the current one.
   *
   * @param startOffset     new start offset value to apply
   */
  protected void arrangeStartOffset(final int startOffset) {
    if (getStartOffset() == startOffset) return;
    boolean isFirst = getParent() != null && getStartOffset() == getParent().getStartOffset();
    myStart = startOffset;
    if (isFirst) {
      getParent().arrangeStartOffset(startOffset);
    }
  }

  public IndentImpl getIndent(){
    return myIndent;
  }

  public CompositeBlockWrapper getParent() {
    return myParent;
  }

  @Nullable
  public WrapImpl getWrap() {
    final ArrayList<WrapImpl> wraps = getWraps();
    if (wraps.size() == 0) return null;
    return wraps.get(0);
  }

  /**
   * @return    wrap object configured for the current block wrapper if any; {@code null} otherwise
   */
  public WrapImpl getOwnWrap() {
    return myWrap;
  }

  public void reset() {
    myFlags |= CAN_USE_FIRST_CHILD_INDENT_AS_BLOCK_INDENT;
    final AlignmentImpl alignment = myAlignment;
    if (alignment != null) alignment.reset();
    final WrapImpl wrap = myWrap;
    if (wrap != null) wrap.reset();
  }

  public IndentData getChildOffset(AbstractBlockWrapper child, CommonCodeStyleSettings.IndentOptions options, int targetBlockStartOffset) {
    final boolean childStartsNewLine = child.getWhiteSpace().containsLineFeeds();
    IndentImpl.Type childIndentType = child.getIndent().getType();
    IndentData childIndent;

    // Calculate child indent.
    if (childStartsNewLine
        || (!getWhiteSpace().containsLineFeeds() && RELATIVE_INDENT_TYPES.contains(childIndentType) && indentAlreadyUsedBefore(child)))
    {
      childIndent = CoreFormatterUtil.getIndent(options, child, targetBlockStartOffset);
    }
    else if (child.getIndent().isEnforceIndentToChildren() && !child.getWhiteSpace().containsLineFeeds()) {
      // Enforce indent if child doesn't start new line, e.g. prefer the code below:
      //    void test() {
      //        foo("test", new Runnable() {
      //                public void run() {
      //                }
      //            },
      //            new Runnable() {
      //                public void run() {
      //                }
      //            }
      //        );
      //    }
      // to this one:
      //    void test() {
      //        foo("test", new Runnable() {
      //            public void run() {
      //            }
      //        },
      //            new Runnable() {
      //                public void run() {
      //                }
      //            }
      //        );
      //    }
      AlignmentImpl alignment = child.getAlignment();
      if (alignment != null) {
        AbstractBlockWrapper anchorBlock = getAnchorBlock(child, targetBlockStartOffset, alignment);
        return anchorBlock.getNumberOfSymbolsBeforeBlock();
      }
      childIndent = CoreFormatterUtil.getIndent(options, child, getStartOffset());
    }
    else {
      childIndent = new IndentData(0);
    }

    // Use child indent if it's absolute and the child is contained on new line.
    if (childStartsNewLine) {
      if (child.getIndent().isAbsolute()) {
        myFlags &= ~CAN_USE_FIRST_CHILD_INDENT_AS_BLOCK_INDENT;
        AbstractBlockWrapper current = this;
        while (current != null && current.getStartOffset() == getStartOffset()) {
          current.myFlags &= ~CAN_USE_FIRST_CHILD_INDENT_AS_BLOCK_INDENT;
          current = current.myParent;
        }
        return childIndent;
      }      
      else if (child.getIndent().isRelativeToDirectParent() && child.getStartOffset() > getStartOffset()) {
        return childIndent.add(getNumberOfSymbolsBeforeBlock());
      }
    }

    if (child.getStartOffset() == getStartOffset()) {
      final boolean newValue = (myFlags & CAN_USE_FIRST_CHILD_INDENT_AS_BLOCK_INDENT) != 0 &&
                               (child.myFlags & CAN_USE_FIRST_CHILD_INDENT_AS_BLOCK_INDENT) != 0 && childIndent.isEmpty();
      setCanUseFirstChildIndentAsBlockIndent(newValue);
    }

    if (getStartOffset() == targetBlockStartOffset) {
      if (myParent == null) {
        return childIndent;
      }
      else {
        return childIndent.add(myParent.getChildOffset(this, options, targetBlockStartOffset));
      }
    }
    else if (!getWhiteSpace().containsLineFeeds()) {
      final IndentData indent = createAlignmentIndent(childIndent, child);
      if (indent != null) {
        return indent;
      }
      return childIndent.add(myParent.getChildOffset(this, options, targetBlockStartOffset));
    }
    else {
      if (myParent == null) return  childIndent.add(getWhiteSpace());
      if (getIndent().isAbsolute()) {
        if (myParent.myParent != null) {
          return childIndent.add(myParent.myParent.getChildOffset(myParent, options, targetBlockStartOffset));
        }
        else {
          return  childIndent.add(getWhiteSpace());
        }
      }
      if ((myFlags & CAN_USE_FIRST_CHILD_INDENT_AS_BLOCK_INDENT) != 0) {
        final IndentData indent = createAlignmentIndent(childIndent, child);
        if (indent != null) {
          return indent;
        }
        return childIndent.add(getWhiteSpace());
      }
      else {
        return childIndent.add(myParent.getChildOffset(this, options, targetBlockStartOffset));
      }
    }
  }

  @NotNull
  private AbstractBlockWrapper getAnchorBlock(AbstractBlockWrapper child, int targetBlockStartOffset, AlignmentImpl alignment) {
    // Generally, we want to handle situation like the one below:
    //   test("text", new Runnable() { 
    //            @Override
    //            public void run() {
    //            }
    //        },
    //        new Runnable() {
    //            @Override
    //            public void run() {
    //            }
    //        }
    //   );
    // I.e. we want 'run()' method from the first anonymous class to be aligned with the 'run()' method of the second anonymous class.

    AbstractBlockWrapper anchorBlock = alignment.getOffsetRespBlockBefore(child);
    if (anchorBlock == null) {
      anchorBlock = this;
      if (anchorBlock instanceof CompositeBlockWrapper) {
        List<AbstractBlockWrapper> children = ((CompositeBlockWrapper)anchorBlock).getChildren();
        for (AbstractBlockWrapper c : children) {
          if (c.getStartOffset() != getStartOffset() && c.getStartOffset() < targetBlockStartOffset) {
            anchorBlock = c;
            break;
          }
        }
      }
    }
    return anchorBlock;
  }

  /**
   * Allows to answer if current wrapped block has a child block that is located before given block and has line feed.
   *
   * @param child   target child block to process
   * @return        {@code true} if current block has a child that is located before the given block and contains line feed;
   *                {@code false} otherwise
   */
  protected abstract boolean indentAlreadyUsedBefore(final AbstractBlockWrapper child);

  /**
   * Allows to retrieve object that encapsulates information about number of symbols before the current block starting
   * from the line start. I.e. all symbols (either white space or not) between start of the line where current block begins
   * and the block itself are count and returned.
   *
   * @return    object that encapsulates information about number of symbols before the current block
   */
  public abstract IndentData getNumberOfSymbolsBeforeBlock();

  /**
   * @return    previous block for the current block if any; {@code null} otherwise
   */
  @Nullable
  protected abstract LeafBlockWrapper getPreviousBlock();

  protected final void setCanUseFirstChildIndentAsBlockIndent(final boolean newValue) {
    if (newValue) myFlags |= CAN_USE_FIRST_CHILD_INDENT_AS_BLOCK_INDENT;
    else myFlags &= ~CAN_USE_FIRST_CHILD_INDENT_AS_BLOCK_INDENT;
  }

  /**
   * Applies start offset of the current block wrapper to the parent block wrapper if the one is defined.
   */
  public void arrangeParentTextRange() {
    if (myParent != null) {
      myParent.arrangeStartOffset(getStartOffset());
    }
  }
  public IndentData calculateChildOffset(final CommonCodeStyleSettings.IndentOptions indentOption, final ChildAttributes childAttributes,
                                         int index) {
    IndentImpl childIndent = (IndentImpl)childAttributes.getChildIndent();

    if (childIndent == null) childIndent = (IndentImpl)Indent.getContinuationWithoutFirstIndent(indentOption.USE_RELATIVE_INDENTS);

    IndentData indent = getIndent(indentOption, index, childIndent);
    if (childIndent.isRelativeToDirectParent()) {
      return new IndentData(indent.getIndentSpaces() + CoreFormatterUtil.getStartColumn(CoreFormatterUtil.getFirstLeaf(this)),
                            indent.getSpaces());
    }
    if (myParent == null || (myFlags & CAN_USE_FIRST_CHILD_INDENT_AS_BLOCK_INDENT) != 0 && getWhiteSpace().containsLineFeeds()) {
      return indent.add(getWhiteSpace());
    }
    else {
      IndentData offsetFromParent = myParent.getChildOffset(this, indentOption, -1);
      return indent.add(offsetFromParent);
    }

  }

  /**
   * Allows to retrieve alignment applied to any block that conforms to the following conditions:
   * <p/>
   * <ul>
   *   <li>that block is current block or its ancestor (direct or indirect parent);</li>
   *   <li>that block starts at the same offset as the current one;</li>
   * </ul>
   *
   * @return    alignment of the current block or it's ancestor that starts at the same offset as the current if any;
   *            {@code null} otherwise
   */
  @Nullable
  public AlignmentImpl getAlignmentAtStartOffset() {
    for (AbstractBlockWrapper block = this; block != null && block.getStartOffset() == getStartOffset(); block = block.getParent()) {
      if (block.getAlignment() != null) {
        return block.getAlignment();
      }
    }
    return null;
  }

  /**
   * Check if it's possible to construct indent for the block that is affected by aligning rules. E.g. there is a possible case
   * that the user configures method call arguments to be aligned and single parameter expression spans more than one line:
   * <p/>
   * <pre>
   *     public void test(String s1, String s2) {}
   *
   *     public void foo() {
   *         test("11"
   *                  + "12"
   *                  + "13",
   *              "21"
   *                  + "22");
   *     }
   * </pre>
   * <p/>
   * Here both composite blocks (<b>"11" + "12" + "13"</b> and <b>"21" + "22"</b>) are aligned as method call argument but their
   * sub-blocks that are located on new lines should also be indented to the point of composite block start.
   * <p/>
   * This method takes care about constructing target absolute indent of the given child block assuming that it's parent
   * (referenced by {@code 'this'}) or it's ancestor that starts at the same offset is aligned.
   *
   * @param indentFromParent    basic indent of given child from the current parent block
   * @param child               child block of the current aligned composite block
   * @return                    absolute indent to use for the given child block of the current composite block if alignment-affected
   *                            indent should be used for it;
   *                            {@code null} otherwise
   */
  @Nullable
  private IndentData createAlignmentIndent(IndentData indentFromParent, AbstractBlockWrapper child) {
    if (!child.getWhiteSpace().containsLineFeeds()) {
      return null;
    }

    AlignmentImpl alignment = getAlignmentAtStartOffset();
    if (alignment == null || alignment == child.getAlignment()) {
      return null;
    }
    
    AbstractBlockWrapper previous = child.getPreviousBlock();
    LeafBlockWrapper anchorOffsetBlock = alignment.getOffsetRespBlockBefore(child);
    if (anchorOffsetBlock != null && anchorOffsetBlock.getStartOffset() != getStartOffset()) {
      // Located on different lines.
      boolean onDifferentLines = false;
      for (LeafBlockWrapper b = anchorOffsetBlock.getNextBlock(); b != null && b.getStartOffset() < getStartOffset(); b = b.getNextBlock()) {
        if (b.getWhiteSpace().containsLineFeeds()) {
          onDifferentLines = true;
          break;
        }
      }

      if (!onDifferentLines) {
        return null;
      }
    }

    // There is no point in continuing processing if given child is the first block, i.e. there is no alignment-implied
    // offset to add to the given 'indent from parent'.
    if (previous == null) {
      return indentFromParent;
    }

    IndentData symbolsBeforeCurrent;
    if (anchorOffsetBlock == null) {
      symbolsBeforeCurrent = getNumberOfSymbolsBeforeBlock();
    }
    else {
      symbolsBeforeCurrent = anchorOffsetBlock.getNumberOfSymbolsBeforeBlock();
    }

    // Result is calculated as a number of symbols between the current composite parent block plus given 'indent from parent'.
    int indentSpaces = symbolsBeforeCurrent.getIndentSpaces() + indentFromParent.getSpaces() + indentFromParent.getIndentSpaces();
    return new IndentData(indentSpaces, symbolsBeforeCurrent.getSpaces());
  }

  private static IndentData getIndent(final CommonCodeStyleSettings.IndentOptions options, final int index, IndentImpl indent) {
    if (indent.getType() == Indent.Type.CONTINUATION) {
      return new IndentData(options.CONTINUATION_INDENT_SIZE);
    }
    if (indent.getType() == Indent.Type.CONTINUATION_WITHOUT_FIRST) {
      if (index != 0) {
        return new IndentData(options.CONTINUATION_INDENT_SIZE);
      }
      else {
        return new IndentData(0);
      }
    }
    if (indent.getType() == Indent.Type.LABEL) return new IndentData(options.LABEL_INDENT_SIZE);
    if (indent.getType() == Indent.Type.NONE) return new IndentData(0);
    if (indent.getType() == Indent.Type.SPACES) return new IndentData(indent.getSpaces(), 0);
    return new IndentData(options.INDENT_SIZE);

  }

  /**
   * Applies given indent value to '{@code indentFromParent'} property of the current wrapped block.
   * <p/>
   * Given value is also applied to '{@code indentFromParent'} properties of all parents of the current wrapped block if the
   * value is defined (not {@code null}).
   * <p/>
   * This property is used later during
   * {@link LeafBlockWrapper#calculateOffset(CommonCodeStyleSettings.IndentOptions)} leaf block offset calculation}.
   *
   * @param indentFromParent    indent value to apply
   */
  public void setIndentFromParent(final IndentInfo indentFromParent) {
    myIndentFromParent = indentFromParent;
    if (myIndentFromParent != null) {
      AbstractBlockWrapper parent = myParent;
      if (myParent != null && myParent.getStartOffset() == myStart) {
        parent.setIndentFromParent(myIndentFromParent);
      }
    }    
  }

  /**
   * Tries to find first parent block of the current block that starts before the current block and which
   * {@link WhiteSpace white space} contains line feeds.
   *
   * @return    first parent block that starts before the current block and which white space contains line feeds if any;
   *            {@code null} otherwise
   */
  @Nullable
  protected AbstractBlockWrapper findFirstIndentedParent() {
    if (myParent == null) return null;
    if (myStart != myParent.getStartOffset() && myParent.getWhiteSpace().containsLineFeeds()) return myParent;
    return myParent.findFirstIndentedParent();
  }

  public void setIndent(@Nullable final IndentImpl indent) {
    myIndent = indent;
  }

  public AlignmentImpl getAlignment() {
    return myAlignment;
  }

  public boolean isIncomplete() {
    return (myFlags & INCOMPLETE) != 0;
  }

  public void dispose() {
    myAlignment = null;
    myWrap = null;
    myIndent = null;
    myIndentFromParent = null;
    myParent = null;
  }

  @Override
  public String toString() {
    return getClass().getName() + "(" + myStart + "-" + myEnd + ")";
  }
}

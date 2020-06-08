// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleConstraints;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains utility methods for core formatter processing.
 */
public final class CoreFormatterUtil {

  private CoreFormatterUtil() {
  }

  @NotNull
  public static FormattingModel buildModel(@NotNull FormattingModelBuilder builder,
                                           @NotNull PsiElement element,
                                           @NotNull TextRange range,
                                           @NotNull CodeStyleSettings settings,
                                           @NotNull FormattingMode mode) {
    if (builder instanceof FormattingModelBuilderEx) {
      return ((FormattingModelBuilderEx)builder).createModel(element, range, settings, mode);
    }
    else {
      return builder.createModel(element, settings);
    }
  }

  @NotNull
  public static FormattingModel buildModel(@NotNull FormattingModelBuilder builder,
                                           @NotNull PsiElement element,
                                           @NotNull CodeStyleSettings settings,
                                           @NotNull FormattingMode mode) {
    return buildModel(builder, element, element.getTextRange(), settings, mode);
  }

  /**
   * Checks if there is an {@link AlignmentImpl} object that should be used during adjusting
   * {@link AbstractBlockWrapper#getWhiteSpace() white space} of the given block.
   *
   * @param block     target block
   * @return          alignment object to use during adjusting white space of the given block if any; {@code null} otherwise
   */
  @Nullable
  public static AlignmentImpl getAlignment(final @NotNull AbstractBlockWrapper block) {
    AbstractBlockWrapper current = block;
    while (true) {
      AlignmentImpl alignment = current.getAlignment();
      if (alignment == null || alignment.getOffsetRespBlockBefore(block) == null) {
        current = current.getParent();
        if (current == null || current.getStartOffset() != block.getStartOffset()) {
          return null;
        }
      }
      else {
        return alignment;
      }
    }
  }

  /**
   * Calculates number of non-line feed symbols before the given wrapped block.
   * <p/>
   * <b>Example:</b>
   * <pre>
   *      whitespace<sub>11</sub> block<sub>11</sub> whitespace<sub>12</sub> block<sub>12</sub>
   *      whitespace<sub>21</sub> block<sub>21</sub> whitespace<sub>22</sub> block<sub>22</sub>
   * </pre>
   * <p/>
   * Suppose this method is called with the wrapped <code>'block<sub>22</sub>'</code> and <code>'whitespace<sub>21</sub>'</code>
   * contains line feeds but <code>'whitespace<sub>22</sub>'</code> is not. This method returns number of symbols
   * from <code>'whitespace<sub>21</sub>'</code> after its last line feed symbol plus number of symbols at
   * <code>block<sub>21</sub></code> plus number of symbols at <code>whitespace<sub>22</sub></code>.
   *
   * @param block target wrapped block to be used at a boundary during counting non-line feed symbols to the left of it
   * @return non-line feed symbols to the left of the given wrapped block
   */
  public static int getStartColumn(@Nullable LeafBlockWrapper block) {
    if (block == null) return -1;

    int result = 0;
    while (true) {
      final WhiteSpace whiteSpace = block.getWhiteSpace();
      result += whiteSpace.getTotalSpaces();
      if (whiteSpace.containsLineFeeds()) return result;
      block = block.getPreviousBlock();
      if (result > CodeStyleConstraints.MAX_RIGHT_MARGIN || block == null) return result;
      result += block.getSymbolsAtTheLastLine();
      if (block.containsLineFeeds()) return result;
    }
  }

  /**
   * Tries to find the closest block that starts before the given block and contains line feeds.
   *
   * @return closest block to the given block that contains line feeds if any; {@code null} otherwise
   */
  @Nullable
  public static AbstractBlockWrapper getIndentedParentBlock(@NotNull AbstractBlockWrapper block) {
    AbstractBlockWrapper current = block.getParent();
    while (current != null) {
      if (current.getStartOffset() != block.getStartOffset() && current.getWhiteSpace().containsLineFeeds()) return current;
      if (current.getParent() != null) {
        AbstractBlockWrapper prevIndented = current.getParent().getPrevIndentedSibling(current);
        if (prevIndented != null) return prevIndented;
      }
      current = current.getParent();
    }
    return null;
  }

    /**
   * It's possible to configure alignment in a way to allow
   * {@link AlignmentFactory#createAlignment(boolean, Alignment.Anchor)}  backward shift}.
   * <p/>
   * <b>Example:</b>
   * <pre>
   *     class Test {
   *         int i;
   *         StringBuilder buffer;
   *     }
   * </pre>
   * <p/>
   * It's possible that blocks {@code 'i'} and {@code 'buffer'} should be aligned. As formatter processes document from
   * start to end that means that requirement to shift block {@code 'i'} to the right is discovered only during
   * {@code 'buffer'} block processing. I.e. formatter returns to the previously processed block ({@code 'i'}), modifies
   * its white space and continues from that location (performs 'backward' shift).
   * <p/>
   * Here is one very important moment - there is a possible case that formatting blocks are configured in a way that they are
   * combined in explicit cyclic graph.
   * <p/>
   * <b>Example:</b>
   * <pre>
   *     blah(bleh(blih,
   *       bloh), bluh);
   * </pre>
   * <p/>
   * Consider that pairs of blocks {@code 'blih'; 'bloh'} and {@code 'bleh', 'bluh'} should be aligned
   * and backward shift is possible for them. Here is how formatter works:
   * <ol>
   *   <li>
   *      Processing reaches <b>'bloh'</b> block. It's aligned to {@code 'blih'} block. Current document state:
   *      <p/>
   *      <pre>
   *          blah(bleh(blih,
   *                    bloh), bluh);
   *      </pre>
   *   </li>
   *   <li>
   *      Processing reaches <b>'bluh'</b> block. It's aligned to {@code 'blih'} block and backward shift is allowed, hence,
   *      {@code 'blih'} block is moved to the right and processing contnues from it. Current document state:
   *      <pre>
   *          blah(            bleh(blih,
   *                    bloh), bluh);
   *      </pre>
   *   </li>
   *   <li>
   *      Processing reaches <b>'bloh'</b> block. It's configured to be aligned to {@code 'blih'} block, hence, it's moved
   *      to the right:
   *      <pre>
   *          blah(            bleh(blih,
   *                                bloh), bluh);
   *      </pre>
   *   </li>
   *   <li>We have endless loop then;</li>
   * </ol>
   * So, that implies that we can't use backward alignment if the blocks are configured in a way that backward alignment
   * appliance produces endless loop. This method encapsulates the logic for checking if backward alignment can be applied.
   *
   * @param first                  the first aligned block
   * @param second                 the second aligned block
   * @param alignmentMappings      block aligned mappings info
   * @return                       {@code true} if backward alignment is possible; {@code false} otherwise
   */
  public static boolean allowBackwardAlignment(@NotNull LeafBlockWrapper first, @NotNull LeafBlockWrapper second,
                                               @NotNull Map<AbstractBlockWrapper, Set<AbstractBlockWrapper>> alignmentMappings)
  {
    Set<AbstractBlockWrapper> blocksBeforeCurrent = new HashSet<>();
    for (
      LeafBlockWrapper previousBlock = second.getPreviousBlock();
      previousBlock != null;
      previousBlock = previousBlock.getPreviousBlock())
    {
      Set<AbstractBlockWrapper> blocks = alignmentMappings.get(previousBlock);
      if (blocks != null) {
        blocksBeforeCurrent.addAll(blocks);
      }

      if (previousBlock.getWhiteSpace().containsLineFeeds()) {
        break;
      }
    }

    for (
      LeafBlockWrapper next = first.getNextBlock();
      next != null && !next.getWhiteSpace().containsLineFeeds();
      next = next.getNextBlock())
    {
      if (blocksBeforeCurrent.contains(next)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Calculates indent for the given block and target start offset according to the given indent options.
   *
   * @param options                 indent options to use
   * @param block                   target wrapped block
   * @param tokenBlockStartOffset   target wrapped block offset
   * @return                        indent to use for the given parameters
   */
  public static IndentData getIndent(CommonCodeStyleSettings.IndentOptions options, AbstractBlockWrapper block,
                                     final int tokenBlockStartOffset)
  {
    final IndentImpl indent = block.getIndent();
    if (indent.getType() == Indent.Type.CONTINUATION) {
      return new IndentData(options.CONTINUATION_INDENT_SIZE);
    }
    if (indent.getType() == Indent.Type.CONTINUATION_WITHOUT_FIRST) {
      if (block.getStartOffset() != block.getParent().getStartOffset() && block.getStartOffset() == tokenBlockStartOffset) {
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

  @NotNull
  public static LeafBlockWrapper getFirstLeaf(@NotNull AbstractBlockWrapper block) {
    if (block instanceof LeafBlockWrapper) {
      return (LeafBlockWrapper)block;
    }
    else {
      return getFirstLeaf(((CompositeBlockWrapper)block).getChildren().get(0));
    }
  }
}

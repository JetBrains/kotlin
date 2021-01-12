// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.formatting;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;

import java.io.PrintStream;
import java.util.List;

/**
 * @author yole
 */
@SuppressWarnings({"HardCodedStringLiteral"})
public final class FormattingModelDumper {
  private FormattingModelDumper() {
  }

  public static void dumpFormattingModel(final Block block, int indent, PrintStream stream) {
    StringBuilder builder = new StringBuilder();
    dumpFormattingModel(block, indent, builder);
    stream.print(builder);
  }

  public static String dumpFormattingModelToString(final Block block) {
    StringBuilder builder = new StringBuilder();
    dumpFormattingModel(block, 0, builder);
    return builder.toString();
  }

  public static void dumpFormattingModel(final Block block, int indent, final StringBuilder builder) {
    if (indent == 0) {
      builder.append("--- FORMATTING MODEL ---\n");
    }
    builder.append(StringUtil.repeatSymbol(' ', indent));
    List<Block> subBlocks = block.getSubBlocks();
    if (subBlocks.isEmpty()) {
      dumpTextBlock(block, builder);
    }
    else {
      dumpBlock(block, builder);
      Block prevBlock = null;
      for(Block subBlock: subBlocks) {
        if (prevBlock != null) {
          Spacing spacing = block.getSpacing(prevBlock, subBlock);
          if (spacing != null) {
            dumpSpacing(spacing, indent+2, builder);
          }
        }
        prevBlock = subBlock;
        dumpFormattingModel(subBlock, indent+2, builder);
      }
    }
  }

  private static void dumpTextBlock(final Block block, final StringBuilder builder) {
    builder.append("\"").append(getBlockText(block)).append("\"");
    dumpBlockProperties(block, builder);
    builder.append("\n");
  }

  private static String getBlockText(final Block block) {
    if (block instanceof ASTBlock && ((ASTBlock)block).getNode() != null) {
      return ((ASTBlock)block).getNode().getText();
    }
    else {
      return "unknown block " + block.getClass();
    }
  }

  private static void dumpBlock(final Block block, final StringBuilder builder) {
    builder.append("<block ");
    if (block instanceof ASTBlock && ((ASTBlock)block).getNode() != null) {
      builder.append(((ASTBlock)block).getNode().getElementType());
    }
    else {
      builder.append(block.getClass().getSimpleName());
    }
    dumpBlockProperties(block, builder);
    builder.append(">\n");
  }

  private static void dumpBlockProperties(final Block block, final StringBuilder blockData) {
    TextRange textRange = block.getTextRange();
    blockData.append(" ").append(textRange.getStartOffset()).append(":").append(textRange.getEndOffset());
    Wrap wrap = block.getWrap();
    if (wrap != null) {
      blockData.append(" ").append(wrap);
    }
    Indent indent = block.getIndent();
    if (indent != null) {
      blockData.append(" ").append(indent);
    }
    final Alignment alignment = block.getAlignment();
    if (alignment != null) {
      blockData.append(" ").append(alignment);
    }
  }

  private static void dumpSpacing(final Spacing spacing, final int indent, final StringBuilder out) {
    out.append(StringUtil.repeatSymbol(' ', indent));
    out.append("spacing: ").append(spacing).append("\n");
  }
}

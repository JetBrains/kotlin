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
package com.intellij.internal.psiView.formattingblocks;

import com.intellij.formatting.Block;
import com.intellij.formatting.templateLanguages.DataLanguageBlockWrapper;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.PlatformColors;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class BlockTreeNode extends SimpleNode {
  private final Block myBlock;

  public BlockTreeNode(Block block, BlockTreeNode parent) {
    super(parent);
    myBlock = block;
  }

  public Block getBlock() {
    return myBlock;
  }

  @NotNull
  @Override
  public BlockTreeNode[] getChildren() {
    return ContainerUtil.map2Array(myBlock.getSubBlocks(), BlockTreeNode.class, block -> new BlockTreeNode(block, this));
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    String name = myBlock.getDebugName();
    if (name == null) name = myBlock.getClass().getSimpleName();
    if (myBlock instanceof DataLanguageBlockWrapper) {
      name += " (" + ((DataLanguageBlockWrapper)myBlock).getOriginal().getClass().getSimpleName() + ")";
    }
    presentation.addText(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);

    if (myBlock.getIndent() != null) {
      presentation.addText(" " + String.valueOf(myBlock.getIndent()).replaceAll("[<>]", " "), SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
    if (myBlock.getAlignment() != null) {
      float d = 1.f * System.identityHashCode(myBlock.getAlignment()) / Integer.MAX_VALUE;
      Color color = new JBColor(Color.HSBtoRGB(1.0f * d, .3f, .7f),
                                Color.HSBtoRGB(1.0f * d, .3f, .8f));
      presentation
        .addText(" " + myBlock.getAlignment(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, color));
    }
    if (myBlock.getWrap() != null) {
      presentation
        .addText(" " + myBlock.getWrap(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, PlatformColors.BLUE));
    }
  }

  @NotNull
  @Override
  public Object[] getEqualityObjects() {
    return new Object[]{myBlock};
  }

  @Override
  public boolean isAlwaysLeaf() {
    return myBlock.isLeaf() && myBlock.getSubBlocks().isEmpty();
  }
}

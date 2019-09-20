package com.intellij.internal.psiView.formattingblocks;

import com.intellij.ui.treeStructure.SimpleTreeStructure;
import org.jetbrains.annotations.NotNull;

public class BlockTreeStructure extends SimpleTreeStructure {
  private BlockTreeNode myRoot;

  @NotNull
  @Override
  public BlockTreeNode getRootElement() {
    return myRoot;
  }

  public void setRoot(BlockTreeNode root) {
    myRoot = root;
  }
}

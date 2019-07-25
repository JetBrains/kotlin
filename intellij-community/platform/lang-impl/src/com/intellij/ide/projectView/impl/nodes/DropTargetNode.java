// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl.nodes;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.psi.PsiFileSystemItem;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import static com.intellij.ui.tree.TreePathUtil.toTreeNodes;

/**
 * @author yole
 */
public interface DropTargetNode {
  boolean canDrop(@NotNull TreeNode[] sourceNodes);

  default boolean canDrop(@NotNull TreePath[] sources) {
    return canDrop(toTreeNodes(sources));
  }

  void drop(@NotNull TreeNode[] sourceNodes, @NotNull DataContext dataContext);

  default void drop(@NotNull TreePath[] sources, @NotNull DataContext dataContext) {
    drop(toTreeNodes(sources), dataContext);
  }

  void dropExternalFiles(PsiFileSystemItem[] sourceFileArray, DataContext dataContext);
}

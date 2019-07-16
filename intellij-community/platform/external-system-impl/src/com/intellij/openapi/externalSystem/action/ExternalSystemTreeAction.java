// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Vladislav.Soroka
 */
public abstract class ExternalSystemTreeAction extends ExternalSystemAction {
  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    return super.isEnabled(e) && getTree(e) != null;
  }

  @Nullable
  protected static JTree getTree(@NotNull AnActionEvent e) {
    return e.getData(ExternalSystemDataKeys.PROJECTS_TREE);
  }

  public static class CollapseAll extends ExternalSystemTreeAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      JTree tree = getTree(e);
      if (tree == null) return;

      TreeUtil.collapseAll(tree, -1);
    }
  }

  public static class ExpandAll extends ExternalSystemTreeAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      JTree tree = getTree(e);
      if (tree == null) return;

      TreeUtil.expandAll(tree);
    }
  }
}


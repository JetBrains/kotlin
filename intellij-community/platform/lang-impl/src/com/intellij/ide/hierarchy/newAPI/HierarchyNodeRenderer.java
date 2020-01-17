// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.hierarchy.newAPI;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public final class HierarchyNodeRenderer extends NodeRenderer {
  public HierarchyNodeRenderer() {
    setOpaque(false);
    setIconOpaque(false);
    setTransparentIconBackground(true);
  }

  @Override
  protected void doPaint(Graphics2D g) {
    super.doPaint(g);
    setOpaque(false);
  }

  @Override
  public void customizeCellRenderer(@NotNull JTree tree, Object value,
                                    boolean selected, boolean expanded, boolean leaf,
                                    int row, boolean hasFocus) {
    Object userObject = TreeUtil.getUserObject(value);
    if (userObject instanceof HierarchyNodeDescriptor) {
      HierarchyNodeDescriptor descriptor = (HierarchyNodeDescriptor)userObject;
      descriptor.getHighlightedText().customize(this);
      setIcon(fixIconIfNeeded(descriptor.getIcon(), selected, hasFocus));
    }
    else {
      super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
    }
  }

  @Override
  protected Icon fixIconIfNeeded(Icon icon, boolean selected, boolean hasFocus) {
    return IconUtil.replaceInnerIcon(super.fixIconIfNeeded(icon, selected, hasFocus),
                                     selected ? AllIcons.General.Modified : AllIcons.General.ModifiedSelected,
                                     selected ? AllIcons.General.ModifiedSelected : AllIcons.General.Modified);
  }
}

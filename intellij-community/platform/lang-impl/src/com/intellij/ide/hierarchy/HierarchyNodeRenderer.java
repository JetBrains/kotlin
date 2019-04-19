/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide.hierarchy;

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

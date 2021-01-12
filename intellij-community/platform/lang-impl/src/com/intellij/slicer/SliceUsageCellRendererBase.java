/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.slicer;

import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.usageView.UsageTreeColors;
import com.intellij.usageView.UsageTreeColorsScheme;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public abstract class SliceUsageCellRendererBase extends ColoredTreeCellRenderer {
  private static final EditorColorsScheme ourColorsScheme = UsageTreeColorsScheme.getInstance().getScheme();
  static final SimpleTextAttributes ourInvalidAttributes = SimpleTextAttributes.fromTextAttributes(ourColorsScheme.getAttributes(UsageTreeColors.INVALID_PREFIX));

  public SliceUsageCellRendererBase() {
    setOpaque(false);
  }

  @Override
  public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    assert value instanceof DefaultMutableTreeNode;
    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
    Object userObject = treeNode.getUserObject();
    if (userObject == null) return;
    if (userObject instanceof MyColoredTreeCellRenderer) {
      MyColoredTreeCellRenderer node = (MyColoredTreeCellRenderer)userObject;
      node.customizeCellRenderer(this, tree, value, selected, expanded, leaf, row, hasFocus);
      if (node instanceof SliceNode) {
        setToolTipText(((SliceNode)node).getPresentation().getTooltip());
      }
    }
    else {
      append(userObject.toString(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }
  }

  public abstract void customizeCellRendererFor(@NotNull SliceUsage sliceUsage);
}


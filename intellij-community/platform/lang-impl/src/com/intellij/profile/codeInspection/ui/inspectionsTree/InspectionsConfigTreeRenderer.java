// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.profile.codeInspection.ui.inspectionsTree;

import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ex.Descriptor;
import com.intellij.codeInspection.ex.GlobalInspectionToolWrapper;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.treetable.TreeTableTree;
import com.intellij.util.ui.PlatformColors;
import com.intellij.util.ui.UIUtil;
import org.jdesktop.swingx.renderer.DefaultTreeRenderer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public abstract class InspectionsConfigTreeRenderer extends DefaultTreeRenderer {
  protected abstract String getFilter();

  @Override
  public Component getTreeCellRendererComponent(JTree tree,
                                                Object value,
                                                boolean selected,
                                                boolean expanded,
                                                boolean leaf,
                                                int row,
                                                boolean hasFocus) {
    final SimpleColoredComponent component = new SimpleColoredComponent();
    if (!(value instanceof InspectionConfigTreeNode)) return component;
    InspectionConfigTreeNode node = (InspectionConfigTreeNode)value;

    boolean reallyHasFocus = ((TreeTableTree)tree).getTreeTable().hasFocus();
    Color background = UIUtil.getTreeBackground(selected, reallyHasFocus);
    UIUtil.changeBackGround(component, background);
    Color foreground =
      selected ? UIUtil.getTreeSelectionForeground(reallyHasFocus) : node.isProperSetting() ? PlatformColors.BLUE : UIUtil.getTreeForeground();

    int style = SimpleTextAttributes.STYLE_PLAIN;
    String hint = null;
    if (node instanceof InspectionConfigTreeNode.Group) {
      style = SimpleTextAttributes.STYLE_BOLD;
    }
    else {
      InspectionConfigTreeNode.Tool toolNode = (InspectionConfigTreeNode.Tool)node;
      hint = getHint(toolNode.getDefaultDescriptor());
    }

    SearchUtil.appendFragments(getFilter(), node.getText(), style, foreground, background, component);
    if (hint != null) {
      component.append(" " + hint, selected ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, foreground) : SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
    component.setForeground(foreground);
    return component;
  }

  @Nullable
  private static String getHint(final Descriptor descriptor) {
    final InspectionToolWrapper toolWrapper = descriptor.getToolWrapper();

    if (toolWrapper.getTool() instanceof InspectionToolWrapperWithHint) {
      return ((InspectionToolWrapperWithHint)toolWrapper.getTool()).getHint();
    }
    if (toolWrapper instanceof LocalInspectionToolWrapper ||
        toolWrapper instanceof GlobalInspectionToolWrapper && !((GlobalInspectionToolWrapper)toolWrapper).worksInBatchModeOnly()) {
      return null;
    }
    return InspectionsBundle.message("inspection.tool.availability.in.tree.node1");
  }
}
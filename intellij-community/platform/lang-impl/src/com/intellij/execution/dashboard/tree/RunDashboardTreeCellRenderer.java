// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.tree;

import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.services.ServiceViewTreeCellRendererBase;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Map;

import static com.intellij.execution.dashboard.RunDashboardCustomizer.NODE_LINKS;

/**
 * @author Konstantin Aleev
 */
public class RunDashboardTreeCellRenderer extends ServiceViewTreeCellRendererBase {
  private RunDashboardRunConfigurationNode myNode;

  @Override
  public void customizeCellRenderer(@NotNull JTree tree,
                                    Object value,
                                    boolean selected,
                                    boolean expanded,
                                    boolean leaf,
                                    int row,
                                    boolean hasFocus) {
    myNode = TreeUtil.getUserObject(RunDashboardRunConfigurationNode.class, value);
    super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
  }

  @Override
  protected Object getTag(String fragment) {
    if (myNode == null) return null;

    Map<Object, Object> links = myNode.getUserData(NODE_LINKS);
    return links == null ? null : links.get(fragment);
  }
}

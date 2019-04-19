// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.ui.layout.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.UiDecorator;
import com.intellij.ui.tabs.impl.JBEditorTabs;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.tabs.impl.TabLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * @author Dennis.Ushakov
 */
public class JBRunnerTabsOld extends JBEditorTabs implements JBRunnerTabsBase {

  public JBRunnerTabsOld(@Nullable Project project, @NotNull ActionManager actionManager, IdeFocusManager focusManager, @NotNull Disposable parent) {
    super(project, actionManager, focusManager, parent);
  }

  @Override
  public boolean useSmallLabels() {
    return true;
  }

  @Override
  public int getToolbarInset() {
    return 0;
  }

  @Override
  public int tabMSize() {
    return 8;
  }

  @Override
  public boolean shouldAddToGlobal(Point point) {
    final TabLabel label = getSelectedLabel();
    if (label == null || point == null) {
      return true;
    }
    final Rectangle bounds = label.getBounds();
    return point.y <= bounds.y + bounds.height;
  }

  @Override
  public Rectangle layout(JComponent c, Rectangle bounds) {
    if (c instanceof Toolbar) {
      bounds.height -= 5;
      return super.layout(c, bounds);
    }
    return super.layout(c, bounds);
  }

  @Override
  public void processDropOver(TabInfo over, RelativePoint relativePoint) {
    final Point point = relativePoint.getPoint(getComponent());
    myShowDropLocation = shouldAddToGlobal(point);
    super.processDropOver(over, relativePoint);
    for (Map.Entry<TabInfo, TabLabel> entry : myInfo2Label.entrySet()) {
      final TabLabel label = entry.getValue();
      if (label.getBounds().contains(point) && myDropInfo != entry.getKey()) {
        select(entry.getKey(), false);
        break;
      }
    }
  }

  @Override
  protected Color getEmptySpaceColor() {
    return UIUtil.getBgFillColor(getParent());
  }

  @Override
  protected TabLabel createTabLabel(TabInfo info) {
    return new MyTabLabel(this, info);
  }

  private static class MyTabLabel extends TabLabel {
    MyTabLabel(JBTabsImpl tabs, final TabInfo info) {
      super(tabs, info);
    }

    @Override
    public void apply(UiDecorator.UiDecoration decoration) {
      setBorder(JBUI.Borders.empty(4));
    }

    @Override
    public void setTabActionsAutoHide(boolean autoHide) {
      super.setTabActionsAutoHide(autoHide);
      apply(null);
    }

    @Override
    public void setTabActions(ActionGroup group) {
      super.setTabActions(group);
      if (myActionPanel != null) {
        final JComponent wrapper = (JComponent)myActionPanel.getComponent(0);
        wrapper.remove(0);
        wrapper.add(Box.createHorizontalStrut(6), BorderLayout.WEST);
      }
    }
  }
}

/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.util;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.ui.GridBag;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * @author Denis Zhdanov
 */
public class TitleWithToolbar extends JPanel {

  public TitleWithToolbar(@NotNull String title,
                          @NotNull String actionGroupId,
                          @NotNull String place,
                          @NotNull JComponent targetComponent)
  {
    super(new GridBagLayout());
    ActionManager actionManager = ActionManager.getInstance();
    ActionGroup group = (ActionGroup)actionManager.getAction(actionGroupId);
    ActionToolbar actionToolbar = actionManager.createActionToolbar(place, group, true);
    actionToolbar.setTargetComponent(targetComponent);
    actionToolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);

    add(new MyTitleComponent(title), new GridBag().weightx(1).anchor(GridBagConstraints.WEST).fillCellHorizontally());
    add(actionToolbar.getComponent(), new GridBag().anchor(GridBagConstraints.CENTER));
  }

  private class MyTitleComponent extends JComponent {

    private final Dimension myMinimumSize;
    @NotNull private final Border myBorder;

    MyTitleComponent(@NotNull String title) {
      myBorder = IdeBorderFactory.createTitledBorder(title);
      Insets insets = myBorder.getBorderInsets(TitleWithToolbar.this);
      myMinimumSize = new Dimension(1, insets.top);
    }

    @Override
    public Dimension getPreferredSize() {
      return myMinimumSize;
    }

    @Override
    public Dimension getMinimumSize() {
      return myMinimumSize;
    }

    @Override
    protected void paintComponent(Graphics g) {
      Rectangle bounds = getBounds();
      myBorder.paintBorder(this, g, bounds.x, bounds.y, bounds.width, bounds.height);
    }
  }
}

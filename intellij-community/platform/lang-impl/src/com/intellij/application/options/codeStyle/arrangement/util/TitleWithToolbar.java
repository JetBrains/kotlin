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

import com.intellij.application.options.codeStyle.arrangement.ArrangementConstants;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
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

    JLabel label = new JLabel(title.startsWith("<html>") ? title : UIUtil.replaceMnemonicAmpersand(title));
    label.setLabelFor(targetComponent);

    GridBag gb = new GridBag().nextLine();
    add(label, gb.anchor(GridBagConstraints.WEST));
    add(new JPanel(), gb.next().weightx(1).fillCellHorizontally());
    add(actionToolbar.getComponent(), gb.next().anchor(GridBagConstraints.CENTER));

    setBorder(JBUI.Borders.empty(12, ArrangementConstants.HORIZONTAL_PADDING, 0, 0));
  }
}

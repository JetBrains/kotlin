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
package com.intellij.application.options.codeStyle.arrangement.group;

import com.intellij.application.options.codeStyle.arrangement.ArrangementConstants;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.application.options.codeStyle.arrangement.util.TitleWithToolbar;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.psi.codeStyle.arrangement.group.ArrangementGroupingRule;
import com.intellij.util.ui.GridBag;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Denis Zhdanov
 */
public class ArrangementGroupingRulesPanel extends JPanel implements DataProvider {

  @NotNull private final ArrangementGroupingRulesControl myControl;

  public ArrangementGroupingRulesPanel(@NotNull ArrangementStandardSettingsManager settingsManager,
                                       @NotNull ArrangementColorsProvider colorsProvider)
  {
    super(new GridBagLayout());

    myControl = new ArrangementGroupingRulesControl(settingsManager, colorsProvider);

    TitleWithToolbar top = new TitleWithToolbar(
      ApplicationBundle.message("arrangement.settings.section.groups"),
      ArrangementConstants.ACTION_GROUP_GROUPING_RULES_CONTROL_TOOLBAR,
      ArrangementConstants.GROUPING_RULES_CONTROL_TOOLBAR_PLACE,
      myControl
    );
    
    add(top, new GridBag().coverLine().fillCellHorizontally().weightx(1));
    add(myControl, new GridBag().fillCell().weightx(1).weighty(1).insets(0, ArrangementConstants.HORIZONTAL_PADDING, 0, 0));
  }

  public void setRules(@Nullable List<? extends ArrangementGroupingRule> rules) {
    myControl.setRules(rules);
  }
  
  @NotNull
  public List<ArrangementGroupingRule> getRules() {
    return myControl.getRules();
  }

  @Nullable
  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    if (ArrangementGroupingRulesControl.KEY.is(dataId)) {
      return myControl;
    }
    return null;
  }
}

/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.match.tokens;

import com.intellij.application.options.codeStyle.arrangement.ArrangementConstants;
import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.application.options.codeStyle.arrangement.match.ArrangementMatchingRulesControl;
import com.intellij.application.options.codeStyle.arrangement.util.TitleWithToolbar;
import com.intellij.ide.ui.customization.CustomizationUtil;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.GridBag;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementRuleAliasesPanel extends JPanel implements DataProvider {
  @NotNull protected final ArrangementRuleAliasControl myControl;

  public ArrangementRuleAliasesPanel(@NotNull ArrangementStandardSettingsManager settingsManager,
                                     @NotNull ArrangementColorsProvider colorsProvider) {
    super(new GridBagLayout());
    setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
    JBScrollPane scrollPane = new JBScrollPane();
    final JViewport viewport = scrollPane.getViewport();
    ArrangementMatchingRulesControl.RepresentationCallback callback = new ArrangementMatchingRulesControl.RepresentationCallback() {
      @Override
      public void ensureVisible(@NotNull Rectangle r) {
        Rectangle visibleRect = viewport.getViewRect();
        if (r.y <= visibleRect.y) {
          return;
        }

        int excessiveHeight = r.y + r.height - (visibleRect.y + visibleRect.height);
        if (excessiveHeight <= 0) {
          return;
        }

        int verticalShift = Math.min(r.y - visibleRect.y, excessiveHeight);
        if (verticalShift > 0) {
          viewport.setViewPosition(new Point(visibleRect.x, visibleRect.y + verticalShift));
        }
      }
    };
    myControl = new ArrangementRuleAliasControl(settingsManager, colorsProvider, callback);
    scrollPane.setViewportView(myControl);
    CustomizationUtil.installPopupHandler(
      myControl, ArrangementConstants.ALIAS_RULE_CONTEXT_MENU, ArrangementConstants.ALIAS_RULE_CONTROL_PLACE
    );

    TitleWithToolbar top = new TitleWithToolbar(
      ApplicationBundle.message("arrangement.settings.section.rule.sequence"),
      ArrangementConstants.ALIAS_RULE_CONTROL_TOOLBAR,
      ArrangementConstants.ALIAS_RULE_CONTROL_TOOLBAR_PLACE,
      myControl
    );
    add(top, new GridBag().coverLine().fillCellHorizontally().weightx(1));
    add(scrollPane, new GridBag().fillCell().weightx(1).weighty(1).insets(0, ArrangementConstants.HORIZONTAL_PADDING, 0, 0));
  }

  @NotNull
  public List<StdArrangementMatchRule> getRuleSequences() {
    return myControl.getRuleSequences();
  }

  public void setRuleSequences(@Nullable Collection<StdArrangementMatchRule> rules) {
    myControl.setRuleSequences(rules);
  }

  @Nullable
  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    if (ArrangementRuleAliasControl.KEY.is(dataId)) {
      return myControl;
    }
    return null;
  }
}

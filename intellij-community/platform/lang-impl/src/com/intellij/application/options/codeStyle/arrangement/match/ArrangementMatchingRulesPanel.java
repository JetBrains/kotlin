/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.match;

import com.intellij.application.options.codeStyle.arrangement.ArrangementConstants;
import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.application.options.codeStyle.arrangement.util.TitleWithToolbar;
import com.intellij.ide.ui.customization.CustomizationUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.psi.codeStyle.arrangement.match.ArrangementSectionRule;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementRuleAliasToken;
import com.intellij.ui.SideBorder;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;

/**
 * @author Denis Zhdanov
 */
public class ArrangementMatchingRulesPanel extends JPanel implements DataProvider {

  @NotNull protected final ArrangementSectionRulesControl myControl;

  public ArrangementMatchingRulesPanel(@NotNull Language language,
                                       @NotNull ArrangementStandardSettingsManager settingsManager,
                                       @NotNull ArrangementColorsProvider colorsProvider)
  {
    super(new GridBagLayout());
    
    JBScrollPane scrollPane = new JBScrollPane();
    scrollPane.putClientProperty(UIUtil.KEEP_BORDER_SIDES, SideBorder.ALL);
    final JViewport viewport = scrollPane.getViewport();
    ArrangementSectionRulesControl.RepresentationCallback callback = new ArrangementSectionRulesControl.RepresentationCallback() {
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
    myControl = createRulesControl(language, settingsManager, colorsProvider, callback);
    scrollPane.setViewportView(myControl);
    scrollPane.setViewportBorder(JBUI.Borders.emptyRight(scrollPane.getVerticalScrollBar().getPreferredSize().width));
    CustomizationUtil.installPopupHandler(
      myControl, ArrangementConstants.ACTION_GROUP_MATCHING_RULES_CONTEXT_MENU, ArrangementConstants.MATCHING_RULES_CONTROL_PLACE
    );

    TitleWithToolbar top = new TitleWithToolbar(
      ApplicationBundle.message("arrangement.settings.section.match"),
      ArrangementConstants.ACTION_GROUP_MATCHING_RULES_CONTROL_TOOLBAR,
      ArrangementConstants.MATCHING_RULES_CONTROL_TOOLBAR_PLACE,
      myControl
    );
    add(top, new GridBag().coverLine().fillCellHorizontally().weightx(1));
    add(scrollPane, new GridBag().fillCell().weightx(1).weighty(1).insets(0, ArrangementConstants.HORIZONTAL_PADDING, 0, 0));
  }

  protected ArrangementSectionRulesControl createRulesControl(@NotNull Language language,
                                                               @NotNull ArrangementStandardSettingsManager settingsManager,
                                                               @NotNull ArrangementColorsProvider colorsProvider,
                                                               @NotNull ArrangementSectionRulesControl.RepresentationCallback callback) {
    return new ArrangementSectionRulesControl(language, settingsManager, colorsProvider, callback);
  }

  @NotNull
  public List<ArrangementSectionRule> getSections() {
    return myControl.getSections();
  }

  public void setSections(@Nullable List<? extends ArrangementSectionRule> rules) {
    myControl.setSections(rules);
  }

  @Nullable
  public Collection<StdArrangementRuleAliasToken> getRulesAliases() {
    return myControl.getRulesAliases();
  }

  public void setRulesAliases(@Nullable Collection<StdArrangementRuleAliasToken> aliases) {
    myControl.setRulesAliases(aliases);
  }

  public void hideEditor() {
    myControl.hideEditor();
  }

  @Nullable
  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    if (ArrangementSectionRulesControl.KEY.is(dataId)) {
      return myControl;
    }
    return null;
  }
}

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
package com.intellij.application.options.codeStyle.arrangement.match.tokens;

import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementRuleAliasToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementRuleAliasConfigurable implements UnnamedConfigurable {
  private final StdArrangementRuleAliasToken myToken;
  private final ArrangementRuleAliasesPanel myTokenRulesPanel;

  public ArrangementRuleAliasConfigurable(@NotNull ArrangementStandardSettingsManager settingsManager,
                                          @NotNull ArrangementColorsProvider colorsProvider,
                                          @NotNull StdArrangementRuleAliasToken token) {
    myToken = token;
    myTokenRulesPanel = new ArrangementRuleAliasesPanel(settingsManager, colorsProvider);
    myTokenRulesPanel.setRuleSequences(token.getDefinitionRules());
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myTokenRulesPanel;
  }

  @Override
  public boolean isModified() {
    final List<StdArrangementMatchRule> newRules = myTokenRulesPanel.getRuleSequences();
    return !newRules.equals(myToken.getDefinitionRules());
  }

  @Override
  public void apply() throws ConfigurationException {
    myToken.setDefinitionRules(myTokenRulesPanel.getRuleSequences());
  }

  @Override
  public void reset() {
    myTokenRulesPanel.setRuleSequences(myToken.getDefinitionRules());
  }
}

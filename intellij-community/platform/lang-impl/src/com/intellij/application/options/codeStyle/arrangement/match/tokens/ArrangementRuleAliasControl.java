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

import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.application.options.codeStyle.arrangement.match.ArrangementMatchingRulesControl;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementRuleAliasControl extends ArrangementMatchingRulesControl {
  @NotNull public static final DataKey<ArrangementRuleAliasControl> KEY = DataKey.create("Arrangement.Alias.Rule.Control");

  public ArrangementRuleAliasControl(@NotNull ArrangementStandardSettingsManager settingsManager,
                                     @NotNull ArrangementColorsProvider colorsProvider,
                                     @NotNull RepresentationCallback callback) {
    super(settingsManager, colorsProvider, callback);
  }

  public List<StdArrangementMatchRule> getRuleSequences() {
    final List<StdArrangementMatchRule> rulesSequences = new ArrayList<>();
    for (int i = 0; i < getModel().getSize(); i++) {
      Object element = getModel().getElementAt(i);
      if (element instanceof StdArrangementMatchRule) {
        rulesSequences.add((StdArrangementMatchRule)element);
      }
    }
    return rulesSequences;
  }

  public void setRuleSequences(Collection<StdArrangementMatchRule> sequences) {
    myComponents.clear();
    getModel().clear();

    if (sequences == null) {
      return;
    }

    for (StdArrangementMatchRule rule : sequences) {
      getModel().add(rule);
    }
  }
}

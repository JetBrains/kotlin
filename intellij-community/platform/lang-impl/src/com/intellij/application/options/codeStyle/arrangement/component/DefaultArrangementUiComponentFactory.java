/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.component;

import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementUiComponent;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokenUiRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Denis Zhdanov
 */
public class DefaultArrangementUiComponentFactory implements ArrangementUiComponent.Factory {

  @Nullable
  @Override
  public ArrangementUiComponent build(@NotNull StdArrangementTokenUiRole role,
                                      @NotNull List<? extends ArrangementSettingsToken> tokens,
                                      @NotNull ArrangementColorsProvider colorsProvider,
                                      @NotNull ArrangementStandardSettingsManager settingsManager)
  {
    switch (role) {
      case CHECKBOX:
        if (tokens.size() != 1) {
          throw new IllegalArgumentException("Can't build a checkbox token for elements " + tokens);
        }
        else {
          return new ArrangementCheckBoxUiComponent(tokens.get(0));
        }
      case COMBO_BOX:
        if (tokens.isEmpty()) {
          throw new IllegalArgumentException("Can't build a combo box token with empty content");
        }
        return new ArrangementComboBoxUiComponent(tokens);
      case LABEL:
        if (tokens.size() != 1) {
          throw new IllegalArgumentException("Can't build a label token for elements " + tokens);
        }
        return new ArrangementLabelUiComponent(tokens.get(0));
      case TEXT_FIELD:
        if (tokens.size() != 1) {
          throw new IllegalArgumentException("Can't build a text field token for elements " + tokens);
        }
        return new ArrangementTextFieldUiComponent(tokens.get(0));
      case BULB:
        if (tokens.size() != 1) {
          throw new IllegalArgumentException("Can't build a bulb token for elements " + tokens);
        }
        return new ArrangementAtomMatchConditionComponent(
          settingsManager, colorsProvider, new ArrangementAtomMatchCondition(tokens.get(0)), null
        );
    }
    return null;
  }
}

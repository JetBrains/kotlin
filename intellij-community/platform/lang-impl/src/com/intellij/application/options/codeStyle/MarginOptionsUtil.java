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
package com.intellij.application.options.codeStyle;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.valueEditors.CommaSeparatedIntegersValueEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

class MarginOptionsUtil {
  public static String getDefaultRightMarginText(@NotNull CodeStyleSettings settings) {
    return getDefaultValueText(Integer.toString(settings.getDefaultRightMargin()));
  }

  static String getDefaultVisualGuidesText(@NotNull CodeStyleSettings settings) {
    List<Integer> softMargins = settings.getDefaultSoftMargins();
    return getDefaultValueText(
      (softMargins.size() > 0
       ? CommaSeparatedIntegersValueEditor.intListToString(settings.getDefaultSoftMargins())
       : ApplicationBundle.message("settings.soft.margins.empty.list")));
  }

  static String getDefaultWrapOnTypingText(@NotNull CodeStyleSettings settings) {
    return getDefaultValueText(settings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN ? "Yes" : "No");
  }

  static void customizeWrapOnTypingCombo(@NotNull JComboBox<String> wrapOnTypingCombo, @NotNull CodeStyleSettings settings) {
    wrapOnTypingCombo.setRenderer(SimpleListCellRenderer.create("", value -> {
      for (int i = 0; i < CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES.length; i++) {
        if (CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES[i] == CommonCodeStyleSettings.WrapOnTyping.DEFAULT.intValue) {
          if (CodeStyleSettingsCustomizable.WRAP_ON_TYPING_OPTIONS[i].equals(value)) {
            return getDefaultWrapOnTypingText(settings);
          }
        }
      }
      return value;
    }));
  }

  static String getDefaultValueText(@NotNull String value) {
    return ApplicationBundle.message("settings.default.value.prefix", value);
  }
}

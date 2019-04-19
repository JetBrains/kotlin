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

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.psi.codeStyle.CodeStyleSettings;

public class OtherFileTypesCodeStyleConfigurable extends CodeStyleAbstractConfigurable {
  public static final String DISPLAY_NAME = ApplicationBundle.message("code.style.other.file.types");

  private final OtherFileTypesCodeStyleOptionsForm myOptionsForm;

  public OtherFileTypesCodeStyleConfigurable(CodeStyleSettings currSettings, CodeStyleSettings modelSettings) {
    super(currSettings, modelSettings, DISPLAY_NAME);
    myOptionsForm = new OtherFileTypesCodeStyleOptionsForm(modelSettings);
  }

  @Override
  protected CodeStyleAbstractPanel createPanel(CodeStyleSettings settings) {
    return myOptionsForm;
  }

  @Override
  public String getHelpTopic() {
        return "settings.editor.codeStyle.other";
      }
}

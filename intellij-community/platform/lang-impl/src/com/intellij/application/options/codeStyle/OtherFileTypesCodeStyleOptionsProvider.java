/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import com.intellij.openapi.options.Configurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.DisplayPriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contains settings for non-language options, for example, text files.
 *
 * @author Rustam Vishnyakov
 */
public class OtherFileTypesCodeStyleOptionsProvider extends CodeStyleSettingsProvider {

  @NotNull
  @Override
  public Configurable createSettingsPage(CodeStyleSettings settings, CodeStyleSettings clonedSettings) {
    return new OtherFileTypesCodeStyleConfigurable(settings, clonedSettings);
  }

  @Nullable
  @Override
  public String getConfigurableDisplayName() {
    return ApplicationBundle.message("code.style.other.file.types");
  }

  @Override
  public DisplayPriority getPriority() {
    return DisplayPriority.OTHER_SETTINGS;
  }
}

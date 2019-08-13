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
package com.intellij.codeInsight.actions;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.Language;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class LastRunReformatCodeOptionsProvider {

  private static final String OPTIMIZE_IMPORTS_KEY     = "LayoutCode.optimizeImports";
  private static final String REARRANGE_ENTRIES_KEY    = "LayoutCode.rearrangeEntries";
  private static final String CODE_CLEANUP_KEY         = "LayoutCode.codeCleanup";
  private static final String PROCESS_CHANGED_TEXT_KEY = "LayoutCode.processChangedText";

  private final PropertiesComponent myPropertiesComponent;

  public LastRunReformatCodeOptionsProvider(@NotNull PropertiesComponent propertiesComponent) {
    myPropertiesComponent = propertiesComponent;
  }

  public ReformatCodeRunOptions getLastRunOptions(@NotNull PsiFile file) {
    Language language = file.getLanguage();

    ReformatCodeRunOptions settings = new ReformatCodeRunOptions(getLastTextRangeType());
    settings.setOptimizeImports(getLastOptimizeImports());
    settings.setRearrangeCode(isRearrangeCode(language));

    return settings;
  }

  public void saveRearrangeState(@NotNull Language language, boolean value) {
    String key = getRearrangeCodeKeyFor(language);
    myPropertiesComponent.setValue(key, Boolean.toString(value));
  }

  public void saveOptimizeImportsState(boolean value) {
    String optimizeImports = Boolean.toString(value);
    myPropertiesComponent.setValue(OPTIMIZE_IMPORTS_KEY, optimizeImports);
  }

  public boolean getLastOptimizeImports() {
    return myPropertiesComponent.getBoolean(OPTIMIZE_IMPORTS_KEY);
  }

  public TextRangeType getLastTextRangeType() {
    return myPropertiesComponent.getBoolean(PROCESS_CHANGED_TEXT_KEY) ? TextRangeType.VCS_CHANGED_TEXT : TextRangeType.WHOLE_FILE;
  }

  public void saveProcessVcsChangedTextState(boolean value) {
    String processOnlyVcsChangedText = Boolean.toString(value);
    myPropertiesComponent.setValue(PROCESS_CHANGED_TEXT_KEY, processOnlyVcsChangedText);
  }

  public void saveRearrangeCodeState(boolean value) {
    myPropertiesComponent.setValue(REARRANGE_ENTRIES_KEY, value);
  }

  public boolean getLastRearrangeCode() {
    return myPropertiesComponent.getBoolean(REARRANGE_ENTRIES_KEY);
  }


  public void saveCodeCleanupState(boolean value) {
    myPropertiesComponent.setValue(CODE_CLEANUP_KEY, value);
  }

  public boolean getLastCodeCleanup() {
    return myPropertiesComponent.getBoolean(CODE_CLEANUP_KEY);
  }

  public boolean isRearrangeCode(@NotNull Language language) {
    String key = getRearrangeCodeKeyFor(language);
    return myPropertiesComponent.getBoolean(key);
  }

  private static String getRearrangeCodeKeyFor(@NotNull Language language) {
    return REARRANGE_ENTRIES_KEY + language.getDisplayName();
  }

}

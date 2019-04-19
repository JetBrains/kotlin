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
package com.intellij.application.options.editor.fonts;

import com.intellij.application.options.editor.EditorOptionsPanel;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontCache;
import com.intellij.openapi.editor.colors.FontPreferences;
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions;
import com.intellij.openapi.editor.colors.impl.EditorColorsManagerImpl;
import com.intellij.openapi.options.Configurable.NoScroll;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.LazyInstance;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AppEditorFontConfigurable implements SearchableConfigurable, NoScroll {

  public static final String ID = "editor.preferences.fonts.default";
  private final LazyInstance<AppEditorFontPanel> myFontPanelInstance = new LazyInstance<AppEditorFontPanel>() {
    @Override
    protected Class<AppEditorFontPanel> getInstanceClass() throws ClassNotFoundException {
      return AppEditorFontPanel.class;
    }
  };

  @NotNull
  @Override
  public String getId() {
    return ID;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return getFontPanel().getComponent();
  }

  @Override
  public boolean isModified() {
    getFontPanel().getOptionsPanel().updateWarning();
    return !getStoredPreferences().equals(getUIFontPreferences());
  }

  @Override
  public void apply() throws ConfigurationException {
    FontPreferences fontPreferences = getUIFontPreferences();
    fontPreferences.copyTo(getStoredPreferences());
    EditorFontCache.getInstance().reset();
    ((EditorColorsManagerImpl)EditorColorsManager.getInstance()).schemeChangedOrSwitched(null);
    EditorOptionsPanel.reinitAllEditors();
  }

  @NotNull
  private FontPreferences getUIFontPreferences() {
    return getFontPanel().getOptionsPanel().getFontPreferences();
  }

  @Override
  public void reset() {
    getStoredPreferences().copyTo(getUIFontPreferences());
    getFontPanel().getOptionsPanel().updateOnChangedFont();
  }

  @NotNull
  private static FontPreferences getStoredPreferences() {
    return AppEditorFontOptions.getInstance().getFontPreferences();
  }

  @NotNull
  private AppEditorFontPanel getFontPanel() {
    return myFontPanelInstance.getValue();
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Font";
  }

  @Override
  public void disposeUIResources() {
    if (myFontPanelInstance.isComputed()) {
      Disposer.dispose(getFontPanel());
    }
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.editor.colors";
  }
}

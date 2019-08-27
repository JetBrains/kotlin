// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor.fonts;

import com.intellij.application.options.editor.EditorOptionsPanel;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontCache;
import com.intellij.openapi.editor.colors.FontPreferences;
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions;
import com.intellij.openapi.editor.colors.impl.EditorColorsManagerImpl;
import com.intellij.openapi.options.Configurable.NoScroll;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.LazyInstance;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class AppEditorFontConfigurable implements SearchableConfigurable, NoScroll {
  public static final String ID = "editor.preferences.fonts.default";
  private final LazyInstance<AppEditorFontPanel> myFontPanelInstance = new LazyInstance<AppEditorFontPanel>() {
    @Override
    protected Class<AppEditorFontPanel> getInstanceClass() {
      return AppEditorFontPanel.class;
    }
  };

  @NotNull
  @Override
  public String getId() {
    return ID;
  }

  @NotNull
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
  public void apply() {
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

  @NotNull
  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.editor.colors";
  }
}

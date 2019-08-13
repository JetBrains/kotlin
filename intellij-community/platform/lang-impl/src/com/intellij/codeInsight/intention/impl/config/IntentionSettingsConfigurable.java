// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.config;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.options.MasterDetails;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.DetailsComponent;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class IntentionSettingsConfigurable implements SearchableConfigurable, MasterDetails, IntentionsConfigurable {
  private IntentionSettingsPanel myPanel;
  @NonNls public static final String HELP_ID = "preferences.intentionPowerPack";
  public static final String DISPLAY_NAME = CodeInsightBundle.message("intention.settings");

  @Override
  public JComponent createComponent() {
    if (myPanel == null) {
      myPanel = new IntentionSettingsPanel();
    }
    JPanel component = myPanel.getComponent();
    component.setPreferredSize(JBUI.size(800, 600));
    component.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    return component;
  }

  @Override
  public void initUi() {
    if (myPanel == null) {
      myPanel = new IntentionSettingsPanel();
    }
    myPanel.initUi();
  }

  @Override
  public JComponent getToolbar() {
    return myPanel.getToolbar();
  }

  @Override
  public JComponent getMaster() {
    return myPanel.getMaster();
  }

  @Override
  public DetailsComponent getDetails() {
    return myPanel.getDetails();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myPanel == null ? null : myPanel.getIntentionTree();
  }

  @Override
  public boolean isModified() {
    return myPanel != null && myPanel.isModified();
  }

  @Override
  public String getDisplayName() {
    return DISPLAY_NAME;
  }

  @Override
  public void reset() {
    myPanel.reset();
  }

  @Override
  public void apply() {
    myPanel.apply();
  }

  @Override
  public void disposeUIResources() {
    if (myPanel != null) {
      myPanel.dispose();
    }
    myPanel = null;
  }

  @Override
  public String getHelpTopic() {
    return HELP_ID;
  }

  @Override
  public Runnable enableSearch(String option) {
    return myPanel.showOption(option);
  }

  @Override
  @NotNull
  public String getId() {
    return HELP_ID;
  }

  @Override
  public void selectIntention(@NotNull String familyName) {
    myPanel.selectIntention(familyName);
  }
}

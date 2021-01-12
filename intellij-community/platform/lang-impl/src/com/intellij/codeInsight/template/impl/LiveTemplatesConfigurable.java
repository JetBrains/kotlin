// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LiveTemplatesConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  static final String ID = "editing.templates";
  private TemplateListPanel myPanel;

  @Override
  public boolean isModified() {
    return myPanel != null && myPanel.isModified();
  }

  @Override
  public JComponent createComponent() {
    myPanel = new TemplateListPanel();
    return myPanel;
  }

  @Override
  public String getDisplayName() {
    return CodeInsightBundle.message("templates.settings.page.title");
  }

  @Override
  public void reset() {
    myPanel.reset();
  }

  @Override
  public void apply() throws ConfigurationException {
    myPanel.apply();
  }

  @Override
  public void disposeUIResources() {
    if (myPanel != null) {
      Disposer.dispose(myPanel);
    }
    myPanel = null;
  }

  @Override
  @NotNull
  public String getHelpTopic() {
    return ID;
  }

  @Override
  @NotNull
  public String getId() {
    return getHelpTopic();
  }

  @Override
  @Nullable
  public Runnable enableSearch(final String option) {
    return () -> myPanel.selectNode(option);
  }

  public TemplateListPanel getTemplateListPanel() {
    return myPanel;
  }

}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options;

import com.intellij.application.options.codeStyle.CodeStyleMainPanel;
import com.intellij.application.options.codeStyle.CodeStyleSettingsPanelFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;
import java.util.Set;

public class CodeStyleConfigurableWrapper
  implements SearchableConfigurable, Configurable.NoMargin, Configurable.NoScroll, OptionsContainingConfigurable {
  private boolean myInitialResetInvoked;
  protected CodeStyleMainPanel myPanel;
  private final CodeStyleSettingsProvider myProvider;
  private final CodeStyleSettingsPanelFactory myFactory;
  private final CodeStyleSchemesConfigurable myOwner;

  public CodeStyleConfigurableWrapper(@NotNull CodeStyleSettingsProvider provider, @NotNull CodeStyleSettingsPanelFactory factory, CodeStyleSchemesConfigurable owner) {
    myProvider = provider;
    myFactory = factory;
    myOwner = owner;
    myInitialResetInvoked = false;
  }

  @Override
  @Nls
  public String getDisplayName() {
    String displayName = myProvider.getConfigurableDisplayName();
    if (displayName != null) return displayName;

    return myPanel != null ? myPanel.getDisplayName() : null;  // fallback for 8.0 API compatibility
  }

  @Override
  public String getHelpTopic() {
    return myPanel != null ? myPanel.getHelpTopic() : null;
  }

  @Override
  public JComponent createComponent() {
    if (myPanel == null) {
      myPanel = new CodeStyleMainPanel(myOwner.getModel(), myFactory, canBeShared());
    }
    return myPanel;
  }

  protected boolean canBeShared() {
    return true;
  }

  @Override
  public boolean isModified() {
    if (myPanel != null) {
      boolean someSchemeModified = myPanel.isModified();
      if (someSchemeModified) {
        myOwner.resetCompleted();
      }
      return someSchemeModified;
    }
    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
    myOwner.apply();
  }

  public void resetPanel() {
    if (myPanel != null) {
      myPanel.reset();
    }
  }

  @Override
  public String toString() {
    return myProvider.getClass().getName();
  }

  @Override
  public void reset() {
    if (!myInitialResetInvoked) {
      try {
        myOwner.resetFromChild();
      }
      finally {
        myInitialResetInvoked = true;
      }
    }
    else {
      myOwner.revert();
    }
  }

  @Override
  @NotNull
  public String getId() {
    return getConfigurableId(getDisplayName());
  }

  @NotNull
  @Override
  public Class<?> getOriginalClass() {
    return myProvider.getClass();
  }

  @Override
  public void disposeUIResources() {
    if (myPanel != null) {
      myPanel.disposeUIResources();
    }
  }

  public boolean isPanelModified() {
    return myPanel != null && myPanel.isModified();
  }

  public void applyPanel() throws ConfigurationException {
    if (myPanel != null) {
      myPanel.apply();
    }
  }

  @NotNull
  @Override
  public Set<String> processListOptions() {
    return getOptionIndexer().processListOptions();
  }

  @Override
  public Map<String, Set<String>> processListOptionsWithPaths() {
    return getOptionIndexer().processListOptionsWithPaths();
  }

  @NotNull
  private OptionsContainingConfigurable getOptionIndexer() {
    if (myPanel == null) {
      myPanel = new CodeStyleMainPanel(myOwner.getModel(), myFactory, canBeShared());
    }
    return myPanel.getOptionIndexer();
  }

  public void selectTab(@NotNull String tab) {
    createComponent();
    myPanel.showTabOnCurrentPanel(tab);
  }

  @NotNull
  public static String getConfigurableId(String configurableDisplayName) {
    return "preferences.sourceCode." + configurableDisplayName;
  }

  @Nullable
  @Override
  public Runnable enableSearch(String option) {
    return myPanel != null ? () -> myPanel.highlightOptions(option) : null;
  }
}

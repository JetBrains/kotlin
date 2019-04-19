// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.settings;

import com.intellij.application.options.CodeCompletionOptionsCustomSection;
import com.intellij.completion.StatsCollectorBundle;
import com.intellij.openapi.options.ConfigurableBase;
import com.intellij.openapi.options.ConfigurableUi;
import com.intellij.openapi.ui.panel.ComponentPanelBuilder;
import com.intellij.ui.components.JBRadioButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CompletionCollectorConfigurable extends ConfigurableBase<CompletionCollectorConfigurable, CompletionStatsCollectorSettings>
  implements ConfigurableUi<CompletionStatsCollectorSettings>, CodeCompletionOptionsCustomSection {
  private JPanel myPanel;
  private JBRadioButton myRbRankAndSend;
  private JBRadioButton myRbNotRankAndNotSend;
  private JBRadioButton myRbRankAndNotSend;
  private JBRadioButton myRbNotRankAndSend;
  private JLabel myDataDetailsLabel;

  CompletionCollectorConfigurable() {
    super("stats.collector.completion.ml", "AI assistant code completion", null);
  }

  @Override
  public void reset(@NotNull CompletionStatsCollectorSettings settings) {
    JBRadioButton shouldBeSelected;
    if (settings.isRankingEnabled()) {
      shouldBeSelected = settings.isCompletionLogsSendAllowed() ? myRbRankAndSend : myRbRankAndNotSend;
    }
    else {
      shouldBeSelected = settings.isCompletionLogsSendAllowed() ? myRbNotRankAndSend : myRbNotRankAndNotSend;
    }
    shouldBeSelected.setSelected(true);
  }

  @Override
  public boolean isModified(@NotNull CompletionStatsCollectorSettings settings) {
    return settings.isCompletionLogsSendAllowed() != isCompletionLogsSendAllowed() || settings.isRankingEnabled() != isRankingEnabled();
  }

  @Override
  public void apply(@NotNull CompletionStatsCollectorSettings settings) {
    settings.setRankingEnabled(isRankingEnabled());
    settings.setCompletionLogsSendAllowed(isCompletionLogsSendAllowed());
  }

  private boolean isCompletionLogsSendAllowed() {
    return myRbNotRankAndSend.isSelected() || myRbRankAndSend.isSelected();
  }

  private boolean isRankingEnabled() {
    return myRbRankAndNotSend.isSelected() || myRbRankAndSend.isSelected();
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @NotNull
  @Override
  protected CompletionStatsCollectorSettings getSettings() {
    return CompletionStatsCollectorSettings.getInstance();
  }

  @Override
  protected CompletionCollectorConfigurable createUi() {
    return new CompletionCollectorConfigurable();
  }

  private void createUIComponents() {
    myDataDetailsLabel =
      ComponentPanelBuilder.createCommentComponent(StatsCollectorBundle.message("ml.completion.data.usage.description"), true);
  }
}

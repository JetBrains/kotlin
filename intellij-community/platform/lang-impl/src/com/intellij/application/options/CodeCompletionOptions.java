// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options;

import com.intellij.application.options.editor.EditorOptionsProvider;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.CompositeConfigurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class CodeCompletionOptions extends CompositeConfigurable<UnnamedConfigurable> implements EditorOptionsProvider {
  private static final ExtensionPointName<CodeCompletionConfigurableEP> EP_NAME = ExtensionPointName.create("com.intellij.codeCompletionConfigurable");

  private CodeCompletionPanel myPanel;

  @Override
  public boolean isModified() {
    return super.isModified() || myPanel != null && myPanel.isModified();
  }

  @Override
  public JComponent createComponent() {
    List<UnnamedConfigurable> configurables = getConfigurables();
    List<JComponent> addonComponents = ContainerUtil.newArrayListWithCapacity(configurables.size());
    List<JComponent> sectionComponents = ContainerUtil.newArrayListWithCapacity(configurables.size());
    for (UnnamedConfigurable configurable : configurables) {
      if (configurable instanceof CodeCompletionOptionsCustomSection) sectionComponents.add(configurable.createComponent());
      else addonComponents.add(configurable.createComponent());
    }
    myPanel = new CodeCompletionPanel(addonComponents, sectionComponents);
    return myPanel.myPanel;
  }

  @Override
  public String getDisplayName() {
    return ApplicationBundle.message("title.code.completion");
  }

  @Override
  public void reset() {
    super.reset();
    myPanel.reset();
  }

  @Override
  public void apply() throws ConfigurationException {
    super.apply();
    myPanel.apply();
  }

  @Override
  public void disposeUIResources() {
    myPanel = null;
    super.disposeUIResources();
  }

  @NotNull
  @Override
  protected List<UnnamedConfigurable> createConfigurables() {
    return ConfigurableWrapper.createConfigurables(EP_NAME);
  }

  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.editor.code.completion";
  }

  @Override
  @NotNull
  public String getId() {
    return "editor.preferences.completion";
  }
}

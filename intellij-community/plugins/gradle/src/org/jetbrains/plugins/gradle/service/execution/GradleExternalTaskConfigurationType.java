// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.execution;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import java.awt.*;

public final class GradleExternalTaskConfigurationType extends AbstractExternalSystemTaskConfigurationType {
  public GradleExternalTaskConfigurationType() {
    super(GradleConstants.SYSTEM_ID);
  }

  @Override
  public String getHelpTopic() {
    return "reference.dialogs.rundebug.GradleRunConfiguration";
  }

  public static GradleExternalTaskConfigurationType getInstance() {
    return (GradleExternalTaskConfigurationType)ExternalSystemUtil.findConfigurationType(GradleConstants.SYSTEM_ID);
  }

  @NotNull
  @Override
  protected ExternalSystemRunConfiguration doCreateConfiguration(@NotNull ProjectSystemId externalSystemId,
                                                                 @NotNull Project project,
                                                                 @NotNull ConfigurationFactory factory,
                                                                 @NotNull String name) {
    return new GradleRunConfiguration(project, factory, name);
  }
}


class GradleDebugSettingsEditor extends SettingsEditor<GradleRunConfiguration> {
  private JCheckBox myCheckBox;

  @Override
  protected void resetEditorFrom(@NotNull GradleRunConfiguration s) {
    myCheckBox.setSelected(s.isScriptDebugEnabled());
  }

  @Override
  protected void applyEditorTo(@NotNull GradleRunConfiguration s) {
    s.setScriptDebugEnabled(myCheckBox.isSelected());
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    myCheckBox = new JCheckBox("Enable Gradle script debugging");
    panel.add(myCheckBox);
    return panel;
  }
}

// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.execution;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleBundle;
import org.jetbrains.plugins.gradle.util.GradleConstants;

public class GradleRunConfiguration extends ExternalSystemRunConfiguration {

  public static final String DEBUG_FLAG_NAME = "GradleScriptDebugEnabled";
  public static final Key<Boolean> DEBUG_FLAG_KEY = Key.create("DEBUG_GRADLE_SCRIPT");
  private boolean isScriptDebugEnabled = true;

  public GradleRunConfiguration(Project project, ConfigurationFactory factory, String name) {
    super(GradleConstants.SYSTEM_ID, project, factory, name);
  }

  public boolean isScriptDebugEnabled() {
    return isScriptDebugEnabled;
  }

  public void setScriptDebugEnabled(boolean scriptDebugEnabled) {
    isScriptDebugEnabled = scriptDebugEnabled;
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) {
    putUserData(DEBUG_FLAG_KEY, Boolean.valueOf(isScriptDebugEnabled));
    return super.getState(executor, env);
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    final Element child = element.getChild(DEBUG_FLAG_NAME);
    if (child != null) {
      isScriptDebugEnabled = Boolean.valueOf(child.getText());
    }
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    final Element child = new Element(DEBUG_FLAG_NAME);
    child.setText(String.valueOf(isScriptDebugEnabled));
    element.addContent(child);
  }

  @NotNull
  @Override
  public SettingsEditor<ExternalSystemRunConfiguration> getConfigurationEditor() {
    final SettingsEditor<ExternalSystemRunConfiguration> editor = super.getConfigurationEditor();
    if (editor instanceof SettingsEditorGroup) {
      final SettingsEditorGroup group = (SettingsEditorGroup)editor;
      //noinspection unchecked
      group.addEditor(GradleBundle.message("gradle.settings.title.debug"), new GradleDebugSettingsEditor());
    }
    return editor;
  }
}

package com.intellij.execution.impl;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.options.SettingsEditor;

public interface RunConfigurationSettingsEditor {

  void setOwner(SettingsEditor<RunnerAndConfigurationSettings> owner);
}

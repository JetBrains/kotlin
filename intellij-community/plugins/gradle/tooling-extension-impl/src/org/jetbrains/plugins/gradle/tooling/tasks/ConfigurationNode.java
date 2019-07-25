// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.tasks;

public class ConfigurationNode extends AbstractComponentNode {
  private final String projectPath;
  private final String configurationName;

  public ConfigurationNode(long id, String projectPath, String configurationName) {
    super(id);
    this.projectPath = projectPath;
    this.configurationName = configurationName;
  }

  public String getConfigurationName() {
    return configurationName;
  }

  @Override
  String getDisplayName() {
    return "project " + projectPath + " (" + configurationName + ")";
  }
}

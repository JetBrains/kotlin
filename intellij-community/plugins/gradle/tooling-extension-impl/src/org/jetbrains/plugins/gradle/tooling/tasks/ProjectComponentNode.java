// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.tasks;

public class ProjectComponentNode extends AbstractComponentNode {

  private final String projectPath;

  public ProjectComponentNode(long id, String projectPath) {
    super(id);
    this.projectPath = projectPath;
  }

  public String getProjectPath() {
    return projectPath;
  }

  @Override
  String getDisplayName() {
    return "project " + projectPath;
  }
}

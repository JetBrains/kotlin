// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.tooling.model.ProjectIdentifier;

import java.io.File;

public class InternalProjectIdentifier implements ProjectIdentifier {
  private final InternalBuildIdentifier build;
  private final String projectPath;

  public InternalProjectIdentifier(InternalBuildIdentifier build, String projectPath) {
    this.build = build;
    this.projectPath = projectPath;
  }

  @Override
  public InternalBuildIdentifier getBuildIdentifier() {
    return this.build;
  }

  @Override
  public String getProjectPath() {
    return this.projectPath;
  }

  public File getRootDir() {
    return this.build.getRootDir();
  }

  public String toString() {
    return String.format("project=%s, %s", this.projectPath, this.build);
  }
}

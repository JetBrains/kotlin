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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InternalProjectIdentifier that = (InternalProjectIdentifier)o;
    if (build != null ? !build.equals(that.build) : that.build != null) return false;
    if (projectPath != null ? !projectPath.equals(that.projectPath) : that.projectPath != null) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = build != null ? build.hashCode() : 0;
    result = 31 * result + (projectPath != null ? projectPath.hashCode() : 0);
    return result;
  }
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.tooling.model.GradleTask;

public class InternalGradleTask implements GradleTask {
  private String path;
  private String name;
  private String description;
  private String displayName;
  private String group;
  private boolean isPublic;
  private InternalProjectIdentifier projectIdentifier;
  private InternalGradleProject gradleProject;

  @Override
  public String getPath() {
    return this.path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDisplayName() {
    return this.displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getGroup() {
    return this.group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  @Override
  public boolean isPublic() {
    return this.isPublic;
  }

  public void setPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }

  @Override
  public InternalProjectIdentifier getProjectIdentifier() {
    return this.projectIdentifier;
  }

  public void setProjectIdentifier(InternalProjectIdentifier projectIdentifier) {
    this.projectIdentifier = projectIdentifier;
  }

  @Override
  public InternalGradleProject getProject() {
    return gradleProject;
  }

  public void setGradleProject(InternalGradleProject gradleProject) {
    this.gradleProject = gradleProject;
  }

  public String toString() {
    return "GradleTask{path='" + this.path + "',public=" + this.isPublic + "}";
  }
}

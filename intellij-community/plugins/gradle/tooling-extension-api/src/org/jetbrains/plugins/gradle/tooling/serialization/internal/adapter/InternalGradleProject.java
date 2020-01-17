// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.internal.ImmutableDomainObjectSet;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter.AdapterUtils.wrap;
import static org.jetbrains.plugins.gradle.tooling.util.GradleContainerUtil.emptyDomainObjectSet;

public class InternalGradleProject implements GradleProject {
  private final InternalGradleScript buildScript = new InternalGradleScript();
  private File buildDirectory;
  private File projectDirectory;
  private Set<InternalGradleTask> tasks = emptyDomainObjectSet();
  private String name;
  private String description;
  private InternalProjectIdentifier projectIdentifier;
  private InternalGradleProject parent;
  private Set<InternalGradleProject> children = emptyDomainObjectSet();

  @Override
  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public InternalGradleProject getParent() {
    return this.parent;
  }

  public void setParent(InternalGradleProject parent) {
    this.parent = parent;
  }

  @Override
  public ImmutableDomainObjectSet<InternalGradleProject> getChildren() {
    return wrap(children);
  }

  public void setChildren(List<InternalGradleProject> children) {
    this.children = ImmutableDomainObjectSet.of(children);
  }

  @Override
  public String getPath() {
    return projectIdentifier == null ? null : projectIdentifier.getProjectPath();
  }

  @Override
  public InternalProjectIdentifier getProjectIdentifier() {
    return this.projectIdentifier;
  }

  public String getProjectPath() {
    return projectIdentifier == null ? null : projectIdentifier.getProjectPath();
  }

  public File getRootDir() {
    return projectIdentifier == null ? null : projectIdentifier.getBuildIdentifier().getRootDir();
  }

  public void setProjectIdentifier(InternalProjectIdentifier projectIdentifier) {
    this.projectIdentifier = projectIdentifier;
  }

  @Override
  public InternalGradleProject findByPath(String path) {
    if (path.equals(this.getPath())) {
      return this;
    }
    else {
      Iterator<? extends InternalGradleProject> iterator = this.children.iterator();
      InternalGradleProject found;
      if (!iterator.hasNext()) {
        return null;
      }
      InternalGradleProject child = iterator.next();
      found = child.findByPath(path);
      while (found == null) {
        if (!iterator.hasNext()) {
          return null;
        }
        child = iterator.next();
        found = child.findByPath(path);
      }
      return found;
    }
  }

  public String toString() {
    return "GradleProject{path='" + this.getPath() + '\'' + '}';
  }

  @Override
  public ImmutableDomainObjectSet<? extends InternalGradleTask> getTasks() {
    return wrap(tasks);
  }

  public void setTasks(List<InternalGradleTask> tasks) {
    this.tasks = ImmutableDomainObjectSet.of(tasks);
  }

  @Override
  public File getBuildDirectory() {
    return this.buildDirectory;
  }

  public void setBuildDirectory(File buildDirectory) {
    this.buildDirectory = buildDirectory;
  }

  @Override
  public File getProjectDirectory() {
    return this.projectDirectory;
  }

  public void setProjectDirectory(File projectDirectory) {
    this.projectDirectory = projectDirectory;
  }

  @Override
  public InternalGradleScript getBuildScript() {
    return this.buildScript;
  }
}

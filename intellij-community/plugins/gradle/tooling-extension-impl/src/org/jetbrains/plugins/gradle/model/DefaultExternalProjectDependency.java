// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.gradle.api.artifacts.Dependency;
import org.gradle.internal.impldep.com.google.common.base.Objects;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public class DefaultExternalProjectDependency extends AbstractExternalDependency implements ExternalProjectDependency {
  private static final long serialVersionUID = 1L;

  private String projectPath;
  private String configurationName = Dependency.DEFAULT_CONFIGURATION;
  private Collection<File> projectDependencyArtifacts;
  private Collection<File> projectDependencyArtifactsSources;

  public DefaultExternalProjectDependency() {
  }

  public DefaultExternalProjectDependency(ExternalProjectDependency dependency) {
    super(dependency);
    projectPath = dependency.getProjectPath();
    configurationName = dependency.getConfigurationName();
    projectDependencyArtifacts =
      dependency.getProjectDependencyArtifacts() == null
      ? new ArrayList<File>(0)
      : new ArrayList<File>(dependency.getProjectDependencyArtifacts());
  }

  @Override
  public String getProjectPath() {
    return projectPath;
  }

  public void setProjectPath(String projectPath) {
    this.projectPath = projectPath;
  }

  @Override
  public String getConfigurationName() {
    return configurationName;
  }

  public void setConfigurationName(String configurationName) {
    this.configurationName = configurationName;
    // have to differentiate(using different DefaultExternalDependencyId) project dependencies on different configurations
    if(!Dependency.DEFAULT_CONFIGURATION.equals(configurationName)){
      setClassifier(configurationName);
    }
  }

  @Override
  public Collection<File> getProjectDependencyArtifacts() {
    return projectDependencyArtifacts;
  }

  public void setProjectDependencyArtifacts(Collection<File> projectArtifacts) {
    projectDependencyArtifacts = projectArtifacts;
  }

  @Override
  public Collection<File> getProjectDependencyArtifactsSources() {
    return projectDependencyArtifactsSources;
  }

  public void setProjectDependencyArtifactsSources(Collection<File> projectArtifactsSources) {
    projectDependencyArtifactsSources = projectArtifactsSources;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DefaultExternalProjectDependency)) return false;
    if (!super.equals(o)) return false;
    DefaultExternalProjectDependency that = (DefaultExternalProjectDependency)o;
    return Objects.equal(projectPath, that.projectPath) && Objects.equal(configurationName, that.configurationName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), projectPath, configurationName);
  }

  @Override
  public String toString() {
    return "project dependency '" + projectPath + ", " + configurationName + '\'' ;
  }
}

/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

  private String myProjectPath;
  private String myConfigurationName = Dependency.DEFAULT_CONFIGURATION;
  private Collection<File> myProjectDependencyArtifacts;
  private Collection<File> myProjectDependencyArtifactsSources;

  public DefaultExternalProjectDependency() {
  }

  public DefaultExternalProjectDependency(ExternalProjectDependency dependency) {
    super(dependency);
    myProjectPath = dependency.getProjectPath();
    myConfigurationName = dependency.getConfigurationName();
    myProjectDependencyArtifacts =
      dependency.getProjectDependencyArtifacts() == null
      ? new ArrayList<File>()
      : new ArrayList<File>(dependency.getProjectDependencyArtifacts());
  }

  @Override
  public String getProjectPath() {
    return myProjectPath;
  }

  public void setProjectPath(String projectPath) {
    myProjectPath = projectPath;
  }

  @Override
  public String getConfigurationName() {
    return myConfigurationName;
  }

  public void setConfigurationName(String configurationName) {
    myConfigurationName = configurationName;
    // have to differentiate(using different DefaultExternalDependencyId) project dependencies on different configurations
    if(!Dependency.DEFAULT_CONFIGURATION.equals(configurationName)){
      setClassifier(configurationName);
    }
  }

  @Override
  public Collection<File> getProjectDependencyArtifacts() {
    return myProjectDependencyArtifacts;
  }

  public void setProjectDependencyArtifacts(Collection<File> projectArtifacts) {
    myProjectDependencyArtifacts = projectArtifacts;
  }

  @Override
  public Collection<File> getProjectDependencyArtifactsSources() {
    return myProjectDependencyArtifactsSources;
  }

  public void setProjectDependencyArtifactsSources(Collection<File> projectArtifactsSources) {
    myProjectDependencyArtifactsSources = projectArtifactsSources;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DefaultExternalProjectDependency)) return false;
    if (!super.equals(o)) return false;
    DefaultExternalProjectDependency that = (DefaultExternalProjectDependency)o;
    return Objects.equal(myProjectPath, that.myProjectPath) && Objects.equal(myConfigurationName, that.myConfigurationName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), myProjectPath, myConfigurationName);
  }

  @Override
  public String toString() {
    return "project dependency '" + myProjectPath + ", " + myConfigurationName + '\'' ;
  }
}

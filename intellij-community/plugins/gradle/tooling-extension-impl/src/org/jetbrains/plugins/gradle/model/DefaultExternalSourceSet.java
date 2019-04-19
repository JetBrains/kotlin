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

import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.model.project.IExternalSystemSourceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class DefaultExternalSourceSet implements ExternalSourceSet {
  private static final long serialVersionUID = 1L;

  private String myName;
  private Map<IExternalSystemSourceType, ExternalSourceDirectorySet> mySources;
  private final Collection<ExternalDependency> myDependencies;
  private Collection<File> myArtifacts;
  private String mySourceCompatibility;
  private String myTargetCompatibility;

  public DefaultExternalSourceSet() {
    mySources = new HashMap<IExternalSystemSourceType, ExternalSourceDirectorySet>();
    myDependencies = new LinkedHashSet<ExternalDependency>();
    myArtifacts = new ArrayList<File>();
  }

  public DefaultExternalSourceSet(ExternalSourceSet sourceSet) {
    this();
    myName = sourceSet.getName();
    mySourceCompatibility = sourceSet.getSourceCompatibility();
    myTargetCompatibility = sourceSet.getTargetCompatibility();
    for (Map.Entry<IExternalSystemSourceType, ExternalSourceDirectorySet> entry : sourceSet.getSources().entrySet()) {
      mySources.put(ExternalSystemSourceType.from(entry.getKey()), new DefaultExternalSourceDirectorySet(entry.getValue()));
    }

    for (ExternalDependency dependency : sourceSet.getDependencies()) {
      myDependencies.add(ModelFactory.createCopy(dependency));
    }
    myArtifacts = sourceSet.getArtifacts() == null ? new ArrayList<File>() : new ArrayList<File>(sourceSet.getArtifacts());
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @Override
  public Collection<File> getArtifacts() {
    return myArtifacts;
  }

  public void setArtifacts(Collection<File> artifacts) {
    myArtifacts = artifacts;
  }

  @Nullable
  @Override
  public String getSourceCompatibility() {
    return mySourceCompatibility;
  }

  public void setSourceCompatibility(@Nullable String sourceCompatibility) {
    mySourceCompatibility = sourceCompatibility;
  }

  @Nullable
  @Override
  public String getTargetCompatibility() {
    return myTargetCompatibility;
  }

  public void setTargetCompatibility(@Nullable String targetCompatibility) {
    myTargetCompatibility = targetCompatibility;
  }

  @Override
  public Collection<ExternalDependency> getDependencies() {
    return myDependencies;
  }

  public void setName(String name) {
    myName = name;
  }

  @NotNull
  @Override
  public Map<IExternalSystemSourceType, ExternalSourceDirectorySet> getSources() {
    return mySources;
  }

  public void setSources(Map<IExternalSystemSourceType, ExternalSourceDirectorySet> sources) {
    mySources = sources;
  }

  @Override
  public String toString() {
    return "sourceSet '" + myName + '\'' ;
  }
}

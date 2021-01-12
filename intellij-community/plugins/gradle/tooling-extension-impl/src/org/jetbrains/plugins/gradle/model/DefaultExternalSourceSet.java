// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
public final class DefaultExternalSourceSet implements ExternalSourceSet {
  private static final long serialVersionUID = 1L;

  private String name;
  private Map<ExternalSystemSourceType, DefaultExternalSourceDirectorySet> sources;
  private final LinkedHashSet<ExternalDependency> dependencies;
  private Collection<File> artifacts;
  private String sourceCompatibility;
  private String targetCompatibility;
  private boolean isPreview;

  public DefaultExternalSourceSet() {
    sources = new HashMap<ExternalSystemSourceType, DefaultExternalSourceDirectorySet>(0);
    dependencies = new LinkedHashSet<ExternalDependency>(0);
    artifacts = new ArrayList<File>(0);
  }

  public DefaultExternalSourceSet(ExternalSourceSet sourceSet) {
    name = sourceSet.getName();
    sourceCompatibility = sourceSet.getSourceCompatibility();
    targetCompatibility = sourceSet.getTargetCompatibility();
    isPreview = sourceSet.isPreview();

    Set<? extends Map.Entry<? extends IExternalSystemSourceType, ? extends ExternalSourceDirectorySet>> entrySet = sourceSet.getSources().entrySet();
    sources = new HashMap<ExternalSystemSourceType, DefaultExternalSourceDirectorySet>(entrySet.size());
    for (Map.Entry<? extends IExternalSystemSourceType, ? extends ExternalSourceDirectorySet> entry : entrySet) {
      sources.put(ExternalSystemSourceType.from(entry.getKey()), new DefaultExternalSourceDirectorySet(entry.getValue()));
    }

    dependencies = new LinkedHashSet<ExternalDependency>(sourceSet.getDependencies().size());
    for (ExternalDependency dependency : sourceSet.getDependencies()) {
      dependencies.add(ModelFactory.createCopy(dependency));
    }
    artifacts = sourceSet.getArtifacts() == null ? new ArrayList<File>(0) : new ArrayList<File>(sourceSet.getArtifacts());
  }

  @NotNull
  @Override
  public String getName() {
    return name;
  }

  @Override
  public Collection<File> getArtifacts() {
    return artifacts;
  }

  public void setArtifacts(Collection<File> artifacts) {
    this.artifacts = artifacts;
  }

  @Nullable
  @Override
  public String getSourceCompatibility() {
    return sourceCompatibility;
  }

  @Override
  public boolean isPreview() {
    return isPreview;
  }

  public void setPreview(boolean preview) {
    isPreview = preview;
  }

  public void setSourceCompatibility(@Nullable String sourceCompatibility) {
    this.sourceCompatibility = sourceCompatibility;
  }

  @Nullable
  @Override
  public String getTargetCompatibility() {
    return targetCompatibility;
  }

  public void setTargetCompatibility(@Nullable String targetCompatibility) {
    this.targetCompatibility = targetCompatibility;
  }

  @Override
  public Collection<ExternalDependency> getDependencies() {
    return dependencies;
  }

  public void setName(String name) {
    this.name = name;
  }

  @NotNull
  @Override
  public Map<? extends IExternalSystemSourceType, ? extends ExternalSourceDirectorySet> getSources() {
    return sources;
  }

  public void setSources(Map<ExternalSystemSourceType, DefaultExternalSourceDirectorySet> sources) {
    this.sources = sources;
  }

  @Override
  public String toString() {
    return "sourceSet '" + name + '\'' ;
  }
}

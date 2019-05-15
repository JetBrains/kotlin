// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class DefaultExternalProject implements ExternalProject, ExternalProjectPreview {
  @NotNull
  private String id;
  @NotNull
  private String name;
  @NotNull
  private String qName;
  @Nullable
  private String description;
  @NotNull
  private String group;
  @NotNull
  private String version;
  @NotNull
  private Map<String, ExternalProject> childProjects;
  @NotNull
  private File projectDir;
  @NotNull
  private File buildDir;
  @Nullable
  private File buildFile;
  @NotNull
  private Map<String, ExternalTask> tasks;
  @NotNull
  private Map<String, ?> properties;
  @NotNull
  private Map<String, ExternalSourceSet> sourceSets;
  @NotNull
  private String externalSystemId;
  @NotNull
  private Map<String, ExternalPlugin> plugins;
  @NotNull
  private List<File> artifacts;
  @NotNull
  private Map<String, Set<File>> myArtifactsByConfiguration;

  public DefaultExternalProject() {
    childProjects = new HashMap<String, ExternalProject>();
    tasks = new HashMap<String, ExternalTask>();
    properties = new HashMap<String, Object>();
    sourceSets = new HashMap<String, ExternalSourceSet>();
    plugins = new HashMap<String, ExternalPlugin>();
    artifacts = new ArrayList<File>();
    myArtifactsByConfiguration = new HashMap<String, Set<File>>();
  }

  public DefaultExternalProject(@NotNull ExternalProject externalProject) {
    this();

    id = externalProject.getId();
    name = externalProject.getName();
    qName = externalProject.getQName();
    version = externalProject.getVersion();
    group = externalProject.getGroup();
    description = externalProject.getDescription();
    projectDir = externalProject.getProjectDir();
    buildDir = externalProject.getBuildDir();
    buildFile = externalProject.getBuildFile();
    externalSystemId = externalProject.getExternalSystemId();

    for (Map.Entry<String, ExternalProject> entry : externalProject.getChildProjects().entrySet()) {
      childProjects.put(entry.getKey(), new DefaultExternalProject(entry.getValue()));
    }

    for (Map.Entry<String, ExternalTask> entry : externalProject.getTasks().entrySet()) {
      tasks.put(entry.getKey(), new DefaultExternalTask(entry.getValue()));
    }
    for (Map.Entry<String, ExternalSourceSet> entry : externalProject.getSourceSets().entrySet()) {
      sourceSets.put(entry.getKey(), new DefaultExternalSourceSet(entry.getValue()));
    }
    for (Map.Entry<String, ExternalPlugin> entry : externalProject.getPlugins().entrySet()) {
      plugins.put(entry.getKey(), new DefaultExternalPlugin(entry.getValue()));
    }

    artifacts.addAll(externalProject.getArtifacts());
    myArtifactsByConfiguration.putAll(externalProject.getArtifactsByConfiguration());
  }

  @NotNull
  @Override
  public String getExternalSystemId() {
    return externalSystemId;
  }

  @NotNull
  @Override
  public String getId() {
    return id;
  }

  public void setId(@NotNull String id) {
    this.id = id;
  }

  public void setExternalSystemId(@NotNull String externalSystemId) {
    this.externalSystemId = externalSystemId;
  }

  @NotNull
  @Override
  public String getName() {
    return name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  @NotNull
  @Override
  public String getQName() {
    return qName;
  }

  public void setQName(@NotNull String QName) {
    qName = QName;
  }

  @Nullable
  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  @NotNull
  @Override
  public String getGroup() {
    return group;
  }

  public void setGroup(@NotNull String group) {
    this.group = group;
  }

  @NotNull
  @Override
  public String getVersion() {
    return version;
  }

  public void setVersion(@NotNull String version) {
    this.version = version;
  }

  @NotNull
  @Override
  public Map<String, ExternalProject> getChildProjects() {
    return childProjects;
  }

  public void setChildProjects(@NotNull Map<String, ExternalProject> childProjects) {
    this.childProjects = childProjects;
  }

  @NotNull
  @Override
  public File getProjectDir() {
    return projectDir;
  }

  public void setProjectDir(@NotNull File projectDir) {
    this.projectDir = projectDir;
  }

  @NotNull
  @Override
  public File getBuildDir() {
    return buildDir;
  }

  public void setBuildDir(@NotNull File buildDir) {
    this.buildDir = buildDir;
  }

  @Nullable
  @Override
  public File getBuildFile() {
    return buildFile;
  }

  public void setBuildFile(@Nullable File buildFile) {
    this.buildFile = buildFile;
  }

  @NotNull
  @Override
  public Map<String, ExternalTask> getTasks() {
    return tasks;
  }

  public void setTasks(@NotNull Map<String, ExternalTask> tasks) {
    this.tasks = tasks;
  }

  @NotNull
  @Override
  public Map<String, ExternalPlugin> getPlugins() {
    return plugins;
  }

  public void setPlugins(@NotNull Map<String, ExternalPlugin> plugins) {
    this.plugins = plugins;
  }

  @NotNull
  @Override
  public Map<String, ?> getProperties() {
    return properties;
  }

  public void setProperties(@NotNull Map<String, ?> properties) {
    this.properties = properties;
  }

  @Nullable
  @Override
  public Object getProperty(String name) {
    return properties.get(name);
  }

  @NotNull
  @Override
  public Map<String, ExternalSourceSet> getSourceSets() {
    return sourceSets;
  }

  public void setSourceSets(@NotNull Map<String, ExternalSourceSet> sourceSets) {
    this.sourceSets = sourceSets;
  }

  @NotNull
  @Override
  public List<File> getArtifacts() {
    return artifacts;
  }

  public void setArtifacts(@NotNull List<File> artifacts) {
    this.artifacts = artifacts;
  }

  public void setArtifactsByConfiguration(@NotNull Map<String, Set<File>> artifactsByConfiguration) {
    myArtifactsByConfiguration = artifactsByConfiguration;
  }

  @NotNull
  @Override
  public Map<String, Set<File>> getArtifactsByConfiguration() {
    return myArtifactsByConfiguration;
  }

  @Override
  public String toString() {
    return "project '" + id + "'";
  }
}

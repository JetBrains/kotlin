// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public final class DefaultExternalProject implements ExternalProject, ExternalProjectPreview {
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
  private Map<String, DefaultExternalProject> childProjects;
  @NotNull
  private File projectDir;
  @NotNull
  private File buildDir;
  @Nullable
  private File buildFile;
  @NotNull
  private Map<String, DefaultExternalTask> tasks;
  @NotNull
  private Map<String, ?> properties = new HashMap<String, Object>();
  @NotNull
  private Map<String, DefaultExternalSourceSet> sourceSets;
  @NotNull
  private String externalSystemId;
  @NotNull
  private Map<String, DefaultExternalPlugin> plugins;
  @NotNull
  private List<File> artifacts;
  @NotNull
  private Map<String, Set<File>> artifactsByConfiguration;

  public DefaultExternalProject() {
    childProjects = new HashMap<String, DefaultExternalProject>();
    tasks = new HashMap<String, DefaultExternalTask>();
    sourceSets = new HashMap<String, DefaultExternalSourceSet>();
    plugins = new HashMap<String, DefaultExternalPlugin>();
    artifacts = new ArrayList<File>();
    artifactsByConfiguration = new HashMap<String, Set<File>>();
  }

  public DefaultExternalProject(@NotNull ExternalProject externalProject) {
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

    Map<String, ? extends ExternalProject> externalProjectChildProjects = externalProject.getChildProjects();
    childProjects = new HashMap<String, DefaultExternalProject>(externalProjectChildProjects.size());
    for (Map.Entry<String, ? extends ExternalProject> entry : externalProjectChildProjects.entrySet()) {
      childProjects.put(entry.getKey(), new DefaultExternalProject(entry.getValue()));
    }

    Map<String, ? extends ExternalTask> externalProjectTasks = externalProject.getTasks();
    tasks = new HashMap<String, DefaultExternalTask>(externalProjectTasks.size());
    for (Map.Entry<String, ? extends ExternalTask> entry : externalProjectTasks.entrySet()) {
      this.tasks.put(entry.getKey(), new DefaultExternalTask(entry.getValue()));
    }

    Map<String, ? extends ExternalSourceSet> externalProjectSourceSets = externalProject.getSourceSets();
    sourceSets = new HashMap<String, DefaultExternalSourceSet>(externalProjectSourceSets.size());
    for (Map.Entry<String, ? extends ExternalSourceSet> entry : externalProjectSourceSets.entrySet()) {
      sourceSets.put(entry.getKey(), new DefaultExternalSourceSet(entry.getValue()));
    }

    Map<String, ? extends ExternalPlugin> externalProjectPlugins = externalProject.getPlugins();
    plugins = new HashMap<String, DefaultExternalPlugin>(externalProjectPlugins.size());
    for (Map.Entry<String, ? extends ExternalPlugin> entry : externalProjectPlugins.entrySet()) {
      this.plugins.put(entry.getKey(), new DefaultExternalPlugin(entry.getValue()));
    }

    artifacts = new ArrayList<File>(externalProject.getArtifacts());
    artifactsByConfiguration = new HashMap<String, Set<File>>(externalProject.getArtifactsByConfiguration());
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
  public Map<String, DefaultExternalProject> getChildProjects() {
    return childProjects;
  }

  public void setChildProjects(@NotNull Map<String, DefaultExternalProject> childProjects) {
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
  public Map<String, ? extends ExternalTask> getTasks() {
    return tasks;
  }

  public void setTasks(@NotNull Map<String, DefaultExternalTask> tasks) {
    this.tasks = tasks;
  }

  @NotNull
  @Override
  public Map<String, ? extends ExternalPlugin> getPlugins() {
    return plugins;
  }

  public void setPlugins(@NotNull Map<String, DefaultExternalPlugin> plugins) {
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
  public Map<String, DefaultExternalSourceSet> getSourceSets() {
    return sourceSets;
  }

  public void setSourceSets(@NotNull Map<String, DefaultExternalSourceSet> sourceSets) {
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
    this.artifactsByConfiguration = artifactsByConfiguration;
  }

  @NotNull
  @Override
  public Map<String, Set<File>> getArtifactsByConfiguration() {
    return artifactsByConfiguration;
  }

  @Override
  public String toString() {
    return "project '" + id + "'";
  }
}

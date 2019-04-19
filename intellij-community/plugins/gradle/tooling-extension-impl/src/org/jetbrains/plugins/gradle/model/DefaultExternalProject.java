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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class DefaultExternalProject implements ExternalProject, ExternalProjectPreview {

  private static final long serialVersionUID = 1L;

  @NotNull
  private String myId;
  @NotNull
  private String myName;
  @NotNull
  private String myQName;
  @Nullable
  private String myDescription;
  @NotNull
  private String myGroup;
  @NotNull
  private String myVersion;
  @NotNull
  private Map<String, ExternalProject> myChildProjects;
  @NotNull
  private File myProjectDir;
  @NotNull
  private File myBuildDir;
  @Nullable
  private File myBuildFile;
  @NotNull
  private Map<String, ExternalTask> myTasks;
  @NotNull
  private Map<String, ?> myProperties;
  @NotNull
  private Map<String, ExternalSourceSet> mySourceSets;
  @NotNull
  private String myExternalSystemId;
  @NotNull
  private Map<String, ExternalPlugin> myPlugins;
  @NotNull
  private List<File> myArtifacts;
  @NotNull
  private Map<String, Set<File>> myArtifactsByConfiguration;

  public DefaultExternalProject() {
    myChildProjects = new HashMap<String, ExternalProject>();
    myTasks = new HashMap<String, ExternalTask>();
    myProperties = new HashMap<String, Object>();
    mySourceSets = new HashMap<String, ExternalSourceSet>();
    myPlugins = new HashMap<String, ExternalPlugin>();
    myArtifacts = new ArrayList<File>();
    myArtifactsByConfiguration = new HashMap<String, Set<File>>();
  }

  public DefaultExternalProject(@NotNull ExternalProject externalProject) {
    this();
    myId = externalProject.getId();
    myName = externalProject.getName();
    myQName = externalProject.getQName();
    myVersion = externalProject.getVersion();
    myGroup = externalProject.getGroup();
    myDescription = externalProject.getDescription();
    myProjectDir = externalProject.getProjectDir();
    myBuildDir = externalProject.getBuildDir();
    myBuildFile = externalProject.getBuildFile();
    myExternalSystemId = externalProject.getExternalSystemId();

    for (Map.Entry<String, ExternalProject> entry : externalProject.getChildProjects().entrySet()) {
      myChildProjects.put(entry.getKey(), new DefaultExternalProject(entry.getValue()));
    }

    for (Map.Entry<String, ExternalTask> entry : externalProject.getTasks().entrySet()) {
      myTasks.put(entry.getKey(), new DefaultExternalTask(entry.getValue()));
    }
    for (Map.Entry<String, ExternalSourceSet> entry : externalProject.getSourceSets().entrySet()) {
      mySourceSets.put(entry.getKey(), new DefaultExternalSourceSet(entry.getValue()));
    }
    for (Map.Entry<String, ExternalPlugin> entry : externalProject.getPlugins().entrySet()) {
      myPlugins.put(entry.getKey(), new DefaultExternalPlugin(entry.getValue()));
    }

    myArtifacts.addAll(externalProject.getArtifacts());
    myArtifactsByConfiguration.putAll(externalProject.getArtifactsByConfiguration());
  }


  @NotNull
  @Override
  public String getExternalSystemId() {
    return myExternalSystemId;
  }

  @NotNull
  @Override
  public String getId() {
    return myId;
  }

  public void setId(@NotNull String id) {
    myId = id;
  }

  public void setExternalSystemId(@NotNull String externalSystemId) {
    myExternalSystemId = externalSystemId;
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  public void setName(@NotNull String name) {
    myName = name;
  }

  @NotNull
  @Override
  public String getQName() {
    return myQName;
  }

  public void setQName(@NotNull String QName) {
    myQName = QName;
  }

  @Nullable
  @Override
  public String getDescription() {
    return myDescription;
  }

  public void setDescription(@Nullable String description) {
    myDescription = description;
  }

  @NotNull
  @Override
  public String getGroup() {
    return myGroup;
  }

  public void setGroup(@NotNull String group) {
    myGroup = group;
  }

  @NotNull
  @Override
  public String getVersion() {
    return myVersion;
  }

  public void setVersion(@NotNull String version) {
    myVersion = version;
  }

  @NotNull
  @Override
  public Map<String, ExternalProject> getChildProjects() {
    return myChildProjects;
  }

  public void setChildProjects(@NotNull Map<String, ExternalProject> childProjects) {
    myChildProjects = childProjects;
  }

  @NotNull
  @Override
  public File getProjectDir() {
    return myProjectDir;
  }

  public void setProjectDir(@NotNull File projectDir) {
    myProjectDir = projectDir;
  }

  @NotNull
  @Override
  public File getBuildDir() {
    return myBuildDir;
  }

  public void setBuildDir(@NotNull File buildDir) {
    myBuildDir = buildDir;
  }

  @Nullable
  @Override
  public File getBuildFile() {
    return myBuildFile;
  }

  public void setBuildFile(@Nullable File buildFile) {
    myBuildFile = buildFile;
  }

  @NotNull
  @Override
  public Map<String, ExternalTask> getTasks() {
    return myTasks;
  }

  public void setTasks(@NotNull Map<String, ExternalTask> tasks) {
    myTasks = tasks;
  }

  @NotNull
  @Override
  public Map<String, ExternalPlugin> getPlugins() {
    return myPlugins;
  }

  public void setPlugins(@NotNull Map<String, ExternalPlugin> plugins) {
    myPlugins = plugins;
  }

  @NotNull
  @Override
  public Map<String, ?> getProperties() {
    return myProperties;
  }

  public void setProperties(@NotNull Map<String, ?> properties) {
    myProperties = properties;
  }

  @Nullable
  @Override
  public Object getProperty(String name) {
    return myProperties.get(name);
  }

  @NotNull
  @Override
  public Map<String, ExternalSourceSet> getSourceSets() {
    return mySourceSets;
  }

  public void setSourceSets(@NotNull Map<String, ExternalSourceSet> sourceSets) {
    mySourceSets = sourceSets;
  }

  @NotNull
  @Override
  public List<File> getArtifacts() {
    return myArtifacts;
  }

  public void setArtifacts(@NotNull List<File> artifacts) {
    this.myArtifacts = artifacts;
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
    return "project '" + myId + "'";
  }
}

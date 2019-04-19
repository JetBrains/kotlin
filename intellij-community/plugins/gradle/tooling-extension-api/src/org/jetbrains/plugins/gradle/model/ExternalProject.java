/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import org.gradle.tooling.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public interface ExternalProject extends Model, Serializable {

  @NotNull
  String getExternalSystemId();

  @NotNull
  String getId();

  @NotNull
  String getName();

  @NotNull
  String getQName();

  @Nullable
  String getDescription();

  @NotNull
  String getGroup();

  @NotNull
  String getVersion();

  @NotNull
  Map<String, ExternalProject> getChildProjects();

  @NotNull
  File getProjectDir();

  @NotNull
  File getBuildDir();

  @Nullable
  File getBuildFile();

  @NotNull
  Map<String, ExternalTask> getTasks();

  //@NotNull
  //Map<String, ExternalConfiguration> getConfigurations();

  //@NotNull
  //List<ExternalRepository> getRepositories();

  @NotNull
  Map<String, ExternalPlugin> getPlugins();

  //@NotNull
  //ExternalProjectBuild getBuild();

  @NotNull
  Map<String, ?> getProperties();

  @Nullable
  Object getProperty(String name);

  @NotNull
  Map<String, ExternalSourceSet> getSourceSets();

  /**
   * The paths where the artifacts is constructed
   *
   * @return
   */
  @NotNull
  List<File> getArtifacts();

  /**
   * The artifacts per configuration.
   *
   * @return a mapping between the name of a configuration and the files associated with it.
   */
  @NotNull
  Map<String, Set<File>> getArtifactsByConfiguration();
}

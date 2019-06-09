// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
  Map<String, ? extends ExternalProject> getChildProjects();

  @NotNull
  File getProjectDir();

  @NotNull
  File getBuildDir();

  @Nullable
  File getBuildFile();

  @NotNull
  Map<String, ? extends ExternalTask> getTasks();

  //@NotNull
  //Map<String, ExternalConfiguration> getConfigurations();

  //@NotNull
  //List<ExternalRepository> getRepositories();

  @NotNull
  Map<String, ? extends ExternalPlugin> getPlugins();

  //@NotNull
  //ExternalProjectBuild getBuild();

  @NotNull
  Map<String, ?> getProperties();

  @Nullable
  Object getProperty(String name);

  @NotNull
  Map<String, ? extends ExternalSourceSet> getSourceSets();

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

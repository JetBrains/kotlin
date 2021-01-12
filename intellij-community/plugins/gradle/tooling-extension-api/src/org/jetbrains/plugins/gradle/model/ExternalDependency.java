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
import org.jetbrains.plugins.gradle.ExternalDependencyId;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public interface ExternalDependency extends Serializable {
  /**
   * <p>The group of the dependency.
   *
   * @return dependency group
   */
  String getGroup();

  /**
   * <p>The name of the dependency.
   *
   * @return dependency name
   */
  String getName();

  /**
   * <p>The version of the dependency
   *
   * @return dependency version
   */
  String getVersion();

  /**
   * Returns the {@link ExternalDependencyId} containing the group, the name and the version of this dependency.
   * Contains the same information as {@link #getGroup()}, {@link #getName()} and {@link #getVersion()}
   *
   * @return the dependency identifier
   */
  @NotNull
  ExternalDependencyId getId();


  /**
   * <p>The scope of the dependency
   *
   * @return dependency scope
   */
  String getScope();

  /**
   * <p>The packaging type
   *
   * @return packaging type
   */
  @Nullable
  String getPackaging();

  /**
   * <p>The classifier
   *
   * @return classifier
   */
  @Nullable
  String getClassifier();

  /**
   * <p>Returns the reason why this particular dependency was selected in the result.
   * Useful if multiple candidates were found during dependency resolution.
   *
   * @return the reason for selecting the dependency
   * @deprecated org.gradle.api.artifacts.result.ComponentSelectionReason#getDescription() was deprecated
   */
  @Nullable
  @Deprecated
  String getSelectionReason();

  /**
   * <p>The order of the dependency in it's classpath.
   *
   * @return classpath order
   */
  int getClasspathOrder();

  /**
   * <p>Returns transitive dependencies
   *
   * @return transitive dependencies
   */
  @NotNull
  Collection<ExternalDependency> getDependencies();

  /**
   * Allows to check if current dependency is transitive, i.e. is visible to the module which depends on module that has current dependency.
   * @return {@code true} if current dependency is transitive; {@code false} otherwise
   */
  boolean getExported();
}

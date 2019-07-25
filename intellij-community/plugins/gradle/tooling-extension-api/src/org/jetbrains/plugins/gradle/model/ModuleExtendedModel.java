/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import org.gradle.tooling.model.DomainObjectSet;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @deprecated to be removed in 2018.1
 *
 * @author Vladislav.Soroka
 */
@Deprecated()
public interface ModuleExtendedModel extends Serializable {
  /**
   * The group of the module.
   *
   * @return module group
   */
  String getGroup();

  /**
   * The name of the module.
   *
   * @return module name
   */
  String getName();

  /**
   * The version of the module
   *
   * @return module version
   */
  String getVersion();

  /**
   * The paths where the artifacts is constructed
   *
   * @return
   */
  List<File> getArtifacts();

  /**
   * All IDEA content roots.
   *
   * @return content roots
   */
  DomainObjectSet<? extends ExtIdeaContentRoot> getContentRoots();

  /**
   * The build directory.
   *
   * @return the build directory.
   */
  File getBuildDir();

  /**
   * The compiler output directories.
   *
   * @return the compiler output directories.
   */
  ExtIdeaCompilerOutput getCompilerOutput();

  /**
   * The artifacts per configuration.
   *
   * @return a mapping between the name of a configuration and the files associated with it.
   */
  Map<String, Set<File>> getArtifactsByConfiguration();

  /**
   * Java source compatibility for the module. It may be {@code null}.
   *
   * @return the Java source compatibility for the module, or {@code null} if none was found.
   */
  @Nullable
  String getJavaSourceCompatibility();
}

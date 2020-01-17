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

import java.io.File;
import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public interface ExternalMultiLibraryDependency extends ExternalDependency {
  /**
   * Returns the binary files for this dependency.
   *
   * @return binary file
   */
  @NotNull
  Collection<File> getFiles();

  /**
   * Returns the source directories/archives for this dependency.
   *
   * @return The source files.
   */
  @NotNull
  Collection<File> getSources();

  /**
   * Returns the Javadoc directories/archives for this dependency.
   *
   * @return The Javadoc files.
   */
  @NotNull
  Collection<File> getJavadoc();
}

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

import org.gradle.api.Nullable;

import java.io.File;

/**
 * @author Vladislav.Soroka
 */
public interface ExternalLibraryDependency extends ExternalDependency {
  /**
   * Returns the file for this dependency.
   *
   * @return The file. Never null.
   */
  File getFile();

  /**
   * Returns the source directory/archive for this dependency.
   *
   * @return The source file. Returns null when the source is not available for this dependency.
   */
  @Nullable
  File getSource();

  /**
   * Returns the Javadoc directory/archive for this dependency.
   *
   * @return The Javadoc file. Returns null when the Javadoc is not available for this dependency.
   */
  @Nullable
  File getJavadoc();
}

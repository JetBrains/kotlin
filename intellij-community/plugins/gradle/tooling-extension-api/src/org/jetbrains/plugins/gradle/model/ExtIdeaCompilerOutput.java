/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.io.Serializable;

/**
 * @deprecated to be removed in 2018.1
 */
@Deprecated
public interface ExtIdeaCompilerOutput extends Serializable {
  /**
   * @return the directory to generate the classes of the "main" source set into.
   */
  @Nullable
  File getMainClassesDir();

  /**
   * @return the directory to generate the resources of the "main" source set into.
   */
  @Nullable
  File getMainResourcesDir();

  /**
   * @return the directory to generate the classes of the "test" source set into.
   */
  @Nullable
  File getTestClassesDir();

  /**
   * @return the directory to generate the resources of the "test" source set into.
   */
  @Nullable
  File getTestResourcesDir();
}

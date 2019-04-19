/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import java.io.Serializable;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public interface GradleExtensions extends Serializable {
  @Nullable
  String getParentProjectPath();

  @NotNull
  List<GradleExtension> getExtensions();

  @NotNull
  List<GradleConvention> getConventions();

  @NotNull
  List<GradleProperty> getGradleProperties();

  @NotNull
  List<ExternalTask> getTasks();

  @NotNull
  List<GradleConfiguration> getConfigurations();
}

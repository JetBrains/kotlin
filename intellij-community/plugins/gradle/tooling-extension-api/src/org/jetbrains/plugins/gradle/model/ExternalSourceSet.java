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

import com.intellij.openapi.externalSystem.model.project.IExternalSystemSourceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public interface ExternalSourceSet extends Serializable {
  Collection<File> getArtifacts();

  @NotNull
  String getName();

  @Nullable
  String getSourceCompatibility();

  @Nullable
  String getTargetCompatibility();

  Collection<ExternalDependency> getDependencies();

  @NotNull
  Map<IExternalSystemSourceType, ExternalSourceDirectorySet> getSources();
}

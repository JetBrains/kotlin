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
package org.jetbrains.jps.gradle.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.gradle.model.artifacts.JpsGradleArtifactExtension;
import org.jetbrains.jps.gradle.model.impl.GradleProjectConfiguration;
import org.jetbrains.jps.gradle.model.impl.artifacts.JpsGradleArtifactExtensionImpl;
import org.jetbrains.jps.model.artifact.JpsArtifact;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.service.JpsServiceManager;

/**
 * @author Vladislav.Soroka
 */
public abstract class JpsGradleExtensionService {
  @Nullable
  public static JpsGradleArtifactExtension getArtifactExtension(@NotNull JpsArtifact artifact) {
    return artifact.getContainer().getChild(JpsGradleArtifactExtensionImpl.ROLE);
  }

  public static JpsGradleExtensionService getInstance() {
    return JpsServiceManager.getInstance().getService(JpsGradleExtensionService.class);
  }

  @Nullable
  public abstract JpsGradleModuleExtension getExtension(@NotNull JpsModule module);

  @NotNull
  public abstract JpsGradleModuleExtension getOrCreateExtension(@NotNull JpsModule module, @Nullable String moduleType);

  public abstract void setProductionOnTestDependency(@NotNull JpsDependencyElement dependency, boolean value);

  public abstract boolean isProductionOnTestDependency(@NotNull JpsDependencyElement dependency);

  public abstract boolean hasGradleProjectConfiguration(@NotNull BuildDataPaths paths);

  @NotNull
  public abstract GradleProjectConfiguration getGradleProjectConfiguration(BuildDataPaths paths);
}

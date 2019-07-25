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
package org.jetbrains.plugins.gradle.tooling.internal.ear;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.ExternalDependency;
import org.jetbrains.plugins.gradle.model.ear.EarConfiguration;

import java.util.Collection;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class EarConfigurationImpl implements EarConfiguration {

  @NotNull
  private final List<? extends EarModel> myEarModels;
  @NotNull
  private final Collection<ExternalDependency> myDeployDependencies;
  @NotNull
  private final Collection<ExternalDependency> myEarlibDependencies;

  public EarConfigurationImpl(@NotNull List<? extends EarModel> earModels,
                              @NotNull Collection<ExternalDependency> deployDependencies,
                              @NotNull Collection<ExternalDependency> earlibDependencies) {
    myEarModels = earModels;
    myDeployDependencies = deployDependencies;
    myEarlibDependencies = earlibDependencies;
  }

  @Override
  public List<? extends EarConfiguration.EarModel> getEarModels() {
    return myEarModels;
  }

  @NotNull
  @Override
  public Collection<ExternalDependency> getDeployDependencies() {
    return myDeployDependencies;
  }

  @NotNull
  @Override
  public Collection<ExternalDependency> getEarlibDependencies() {
    return myEarlibDependencies;
  }
}

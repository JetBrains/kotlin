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
package org.jetbrains.plugins.gradle.config;

import com.intellij.openapi.externalSystem.settings.DelegatingExternalSystemSettingsListener;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettingsListener;
import org.jetbrains.plugins.gradle.settings.TestRunner;

/**
 * @author Denis Zhdanov
 */
public class DelegatingGradleSettingsListenerAdapter extends DelegatingExternalSystemSettingsListener<GradleProjectSettings>
  implements GradleSettingsListener
{

  public DelegatingGradleSettingsListenerAdapter(@NotNull ExternalSystemSettingsListener<GradleProjectSettings> delegate) {
    super(delegate);
  }

  @Override
  public void onGradleHomeChange(@Nullable String oldPath, @Nullable String newPath, @NotNull String linkedProjectPath) {
  }

  @Override
  public void onGradleDistributionTypeChange(DistributionType currentValue, @NotNull String linkedProjectPath) {
  }

  @Override
  public void onServiceDirectoryPathChange(@Nullable String oldPath, @Nullable String newPath) {
  }

  @Override
  public void onGradleVmOptionsChange(@Nullable String oldOptions, @Nullable String newOptions) {
  }

  @Override
  public void onBuildDelegationChange(boolean delegatedBuild, @NotNull String linkedProjectPath) {
  }

  @Override
  public void onTestRunnerChange(@NotNull TestRunner currentTestRunner, @NotNull String linkedProjectPath) {
  }
}

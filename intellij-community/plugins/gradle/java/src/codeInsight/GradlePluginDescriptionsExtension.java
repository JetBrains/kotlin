/*
 * Copyright (C) 2015 The Android Open Source Project
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
package org.jetbrains.plugins.gradle.codeInsight;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Extension point to provide Gradle plugin names and their descriptions to be used by
 * {@link org.jetbrains.plugins.gradle.codeInsight.actions.AddGradleDslPluginAction}
 */
public interface GradlePluginDescriptionsExtension {
  ExtensionPointName<GradlePluginDescriptionsExtension> EP_NAME =
    ExtensionPointName.create("org.jetbrains.plugins.gradle.pluginDescriptions");

  /**
   * @return A map from Gradle plugin names to their descriptions.
   */
  @NotNull
  Map<String, String> getPluginDescriptions();
}

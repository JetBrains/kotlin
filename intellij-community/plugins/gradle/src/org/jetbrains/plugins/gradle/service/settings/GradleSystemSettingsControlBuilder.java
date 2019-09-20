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
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleSettings;

/**
 * @author Vladislav.Soroka
 */
public interface GradleSystemSettingsControlBuilder {
  void fillUi(@NotNull PaintAwarePanel canvas, int indentLevel);

  void showUi(boolean show);

  void reset();

  boolean isModified();

  void apply(@NotNull GradleSettings settings);

  boolean validate(@NotNull GradleSettings settings);

  void disposeUIResources();

  @NotNull
  GradleSettings getInitialSettings();
}

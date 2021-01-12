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
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleSettings;

/**
 * Manages gradle settings not specific to particular project (e.g. 'use wrapper' is project-level setting but 'gradle user home' is
 * a global one).
 * 
 * @author Denis Zhdanov
 */
public class GradleSystemSettingsControl implements ExternalSystemSettingsControl<GradleSettings> {

  private final GradleSystemSettingsControlBuilder myBuilder;


  public GradleSystemSettingsControl(@NotNull GradleSettings settings) {
    this(GradleSettingsControlProvider.get().getSystemSettingsControlBuilder(settings));
  }

  public GradleSystemSettingsControl(@NotNull GradleSystemSettingsControlBuilder builder) {
    myBuilder = builder;
  }

  @Override
  public void fillUi(@NotNull PaintAwarePanel canvas, int indentLevel) {
    myBuilder.fillUi(canvas, indentLevel);
  }

  @Override
  public void showUi(boolean show) {
    myBuilder.showUi(show);
  }

  @Override
  public void reset() {
    myBuilder.reset();
  }

  @Override
  public boolean isModified() {
    return myBuilder.isModified();
  }

  @Override
  public void apply(@NotNull GradleSettings settings) {
    myBuilder.apply(settings);
  }

  @Override
  public boolean validate(@NotNull GradleSettings settings) throws ConfigurationException {
    return myBuilder.validate(settings);
  }

  @Override
  public void disposeUIResources() {
    myBuilder.disposeUIResources();
  }

  @NotNull
  public GradleSettings getInitialSettings() {
    return myBuilder.getInitialSettings();
  }
}

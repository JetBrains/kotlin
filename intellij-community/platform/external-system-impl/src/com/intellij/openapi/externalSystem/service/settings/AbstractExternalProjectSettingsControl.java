/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.settings;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Templates class for managing single external project settings (single ide project might contain multiple bindings to external
 * projects, e.g. one module is backed by a single external project and couple of others are backed by a single external multi-project).
 *
 * @author Denis Zhdanov
 */
public abstract class AbstractExternalProjectSettingsControl<S extends ExternalProjectSettings>
  extends AbstractSettingsControl
  implements ExternalSystemSettingsControl<S> {

  @NotNull private final S myInitialSettings;

  protected AbstractExternalProjectSettingsControl(@NotNull S initialSettings) {
    this(null, initialSettings);
  }

  protected AbstractExternalProjectSettingsControl(@Nullable Project project, @NotNull S initialSettings) {
    super(project);
    myInitialSettings = initialSettings;
  }

  /**
   * @deprecated see {@link ExternalSystemSettingsControlCustomizer} for details
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
  protected AbstractExternalProjectSettingsControl(@Nullable Project project,
                                                   @NotNull S initialSettings,
                                                   @Nullable ExternalSystemSettingsControlCustomizer controlCustomizer) {
    this(project, initialSettings);
  }

  @NotNull
  public S getInitialSettings() {
    return myInitialSettings;
  }

  @Override
  public void fillUi(@NotNull PaintAwarePanel canvas, int indentLevel) {
    fillExtraControls(canvas, indentLevel);
  }
  
  protected abstract void fillExtraControls(@NotNull PaintAwarePanel content, int indentLevel);

  @Override
  public boolean isModified() {
    return isExtraSettingModified();
  }

  protected abstract boolean isExtraSettingModified();

  @Override
  public void reset() {
    reset(false, null);
  }

  @Override
  public void reset(@Nullable Project project) {
    reset(false, project);
  }

  @Override
  public void reset(@Nullable WizardContext wizardContext) {
    reset(false, wizardContext, null);
  }

  public void reset(boolean isDefaultModuleCreation, @Nullable Project project) {
    reset(isDefaultModuleCreation, null, project);
  }

  public void reset(boolean isDefaultModuleCreation, @Nullable WizardContext wizardContext, @Nullable Project project) {
    super.reset(wizardContext, project);
    resetExtraSettings(isDefaultModuleCreation, wizardContext);
  }

  protected abstract void resetExtraSettings(boolean isDefaultModuleCreation);

  protected void resetExtraSettings(boolean isDefaultModuleCreation, @Nullable WizardContext wizardContext) {
    resetExtraSettings(isDefaultModuleCreation);
  }

  @Override
  public void apply(@NotNull S settings) {
    settings.setModules(myInitialSettings.getModules());
    if (myInitialSettings.getExternalProjectPath() != null) {
      settings.setExternalProjectPath(myInitialSettings.getExternalProjectPath());
    }
    applyExtraSettings(settings);
  }

  protected abstract void applyExtraSettings(@NotNull S settings);

  @Override
  public void disposeUIResources() {
    ExternalSystemUiUtil.disposeUi(this);
  }
  
  @Override
  public void showUi(boolean show) {
    ExternalSystemUiUtil.showUi(this, show);
  }

  public void updateInitialSettings() {
    updateInitialExtraSettings();
  }

  protected void updateInitialExtraSettings(){}

  /**
   * see {@linkplain AbstractImportFromExternalSystemControl#setCurrentProject(Project)}
   */
  public void setCurrentProject(@Nullable Project project) {
    setProject(project);
  }

  @Nullable
  @Override
  public Project getProject() {
    return super.getProject();
  }
}

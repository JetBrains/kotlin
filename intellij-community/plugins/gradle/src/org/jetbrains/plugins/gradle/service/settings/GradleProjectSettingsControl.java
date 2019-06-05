// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;

/**
 * @author Denis Zhdanov
 */
public class GradleProjectSettingsControl extends AbstractExternalProjectSettingsControl<GradleProjectSettings> {
  private final GradleProjectSettingsControlBuilder myBuilder;

  public GradleProjectSettingsControl(@NotNull GradleProjectSettings initialSettings) {
    this(GradleSettingsControlProvider.get().getProjectSettingsControlBuilder(initialSettings));
  }

  public GradleProjectSettingsControl(@NotNull GradleProjectSettingsControlBuilder builder) {
    super(null, builder.getInitialSettings(), builder.getExternalSystemSettingsControlCustomizer());
    myBuilder = builder;
  }

  @Override
  protected void fillExtraControls(@NotNull PaintAwarePanel content, int indentLevel) {
    myBuilder.createAndFillControls(content, indentLevel);
  }

  @Override
  public boolean validate(@NotNull GradleProjectSettings settings) throws ConfigurationException {
    return myBuilder.validate(settings);
  }

  @Override
  protected void applyExtraSettings(@NotNull GradleProjectSettings settings) {
    myBuilder.apply(settings);
  }

  @Override
  protected void updateInitialExtraSettings() {
    myBuilder.apply(getInitialSettings());
  }

  @Override
  protected boolean isExtraSettingModified() {
    return myBuilder.isModified();
  }

  @Override
  protected void resetExtraSettings(boolean isDefaultModuleCreation) {
    resetExtraSettings(isDefaultModuleCreation, null);
  }

  @Override
  protected void resetExtraSettings(boolean isDefaultModuleCreation, @Nullable WizardContext wizardContext) {
    myBuilder.reset(getProject(), getInitialSettings(), isDefaultModuleCreation, wizardContext);
  }

  public void update(@Nullable String linkedProjectPath, boolean isDefaultModuleCreation) {
    myBuilder.update(linkedProjectPath, getInitialSettings(), isDefaultModuleCreation);
  }

  @Override
  public void showUi(boolean show) {
    super.showUi(show);
    myBuilder.showUi(show);
  }

  /**
   * see {@linkplain AbstractImportFromExternalSystemControl#setCurrentProject(Project)}
   */
  @Override
  public void setCurrentProject(@Nullable Project project) {
    super.setCurrentProject(project);
  }

  @Override
  public void disposeUIResources() {
    super.disposeUIResources();
    myBuilder.disposeUIResources();
  }
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.openapi.externalSystem.service.settings.AbstractExternalSystemConfigurable;
import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettingsListener;
import org.jetbrains.plugins.gradle.util.GradleConstants;

/**
 * @author Denis Zhdanov
 */
public class GradleConfigurable extends AbstractExternalSystemConfigurable<GradleProjectSettings, GradleSettingsListener, GradleSettings> {

  public static final String DISPLAY_NAME = GradleConstants.SYSTEM_ID.getReadableName();
  public static final String ID = "reference.settingsdialog.project.gradle";
  @NonNls public static final String HELP_TOPIC = ID;

  public GradleConfigurable(@NotNull Project project) {
    super(project, GradleConstants.SYSTEM_ID);
  }

  @NotNull
  @Override
  protected ExternalSystemSettingsControl<GradleProjectSettings> createProjectSettingsControl(@NotNull GradleProjectSettings settings) {
    return new GradleProjectSettingsControl(settings);
  }

  @Nullable
  @Override
  protected ExternalSystemSettingsControl<GradleSettings> createSystemSettingsControl(@NotNull GradleSettings settings) {
    return new GradleSystemSettingsControl(settings);
  }

  @NotNull
  @Override
  protected GradleProjectSettings newProjectSettings() {
    return new GradleProjectSettings();
  }

  @NotNull
  @Override
  public String getId() {
    return ID;
  }

  @NotNull
  @Override
  public String getHelpTopic() {
    return HELP_TOPIC;
  }
}

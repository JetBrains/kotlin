// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.settings;

import com.intellij.openapi.externalSystem.test.TestExternalProjectSettings;
import com.intellij.openapi.externalSystem.test.TestExternalSystemSettings;
import com.intellij.openapi.externalSystem.test.TestExternalSystemSettingsListener;
import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.externalSystem.test.ExternalSystemTestUtil.TEST_EXTERNAL_SYSTEM_ID;

public class TestExternalSystemConfigurable
  extends AbstractExternalSystemConfigurable<TestExternalProjectSettings, TestExternalSystemSettingsListener, TestExternalSystemSettings> {

  public TestExternalSystemConfigurable(@NotNull Project project) {
    super(project, TEST_EXTERNAL_SYSTEM_ID);
  }

  @NotNull
  @Override
  protected ExternalSystemSettingsControl<TestExternalProjectSettings> createProjectSettingsControl(@NotNull TestExternalProjectSettings settings) {
    return new TestExternalProjectSettingsControl(settings);
  }

  @Nullable
  @Override
  protected ExternalSystemSettingsControl<TestExternalSystemSettings> createSystemSettingsControl(@NotNull TestExternalSystemSettings settings) {
    return new TestExternalSystemSettingsControl();
  }

  @NotNull
  @Override
  protected TestExternalProjectSettings newProjectSettings() {
    return null;
  }

  @NotNull
  @Override
  public String getId() {
    return "ES Settings";
  }

  private static class TestExternalSystemSettingsControl implements ExternalSystemSettingsControl<TestExternalSystemSettings> {
    @Override
    public void fillUi(@NotNull PaintAwarePanel canvas, int indentLevel) {

    }

    @Override
    public void reset() {

    }

    @Override
    public boolean isModified() {
      return false;
    }

    @Override
    public void apply(@NotNull TestExternalSystemSettings settings) {

    }

    @Override
    public boolean validate(@NotNull TestExternalSystemSettings settings) throws ConfigurationException {
      return false;
    }

    @Override
    public void disposeUIResources() {

    }

    @Override
    public void showUi(boolean show) {

    }
  }
}

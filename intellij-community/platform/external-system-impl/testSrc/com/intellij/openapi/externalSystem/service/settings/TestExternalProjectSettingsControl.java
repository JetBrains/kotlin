// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.settings;

import com.intellij.openapi.externalSystem.test.TestExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import org.jetbrains.annotations.NotNull;

public class TestExternalProjectSettingsControl extends AbstractExternalProjectSettingsControl<TestExternalProjectSettings> {
  public TestExternalProjectSettingsControl(TestExternalProjectSettings settings) {super(settings);}

  @Override
  protected void fillExtraControls(@NotNull PaintAwarePanel content, int indentLevel) {

  }

  @Override
  protected boolean isExtraSettingModified() {
    return false;
  }

  @Override
  protected void resetExtraSettings(boolean isDefaultModuleCreation) {

  }

  @Override
  protected void applyExtraSettings(@NotNull TestExternalProjectSettings settings) {

  }

  @Override
  public boolean validate(@NotNull TestExternalProjectSettings settings) {
    return false;
  }
}

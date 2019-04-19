// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.settings;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractSettingsControl {

  private @Nullable Project myProject;

  AbstractSettingsControl(@Nullable Project project) {
    myProject = project;
  }

  AbstractSettingsControl() {
    this(null);
  }

  @Nullable
  protected Project getProject() {
    return myProject;
  }

  protected void setProject(@Nullable Project project) {
    myProject = project;
  }

  void reset(@Nullable WizardContext wizardContext, @Nullable Project project) {
    myProject = wizardContext == null ? project : wizardContext.getProject();
  }
}

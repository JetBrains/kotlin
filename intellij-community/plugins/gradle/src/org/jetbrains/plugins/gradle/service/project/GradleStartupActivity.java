// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings;

final class GradleStartupActivity implements StartupActivity.DumbAware {
  @Override
  public void runActivity(@NotNull final Project project) {
    ExternalProjectsManager.getInstance(project).runWhenInitialized(() -> GradleExtensionsSettings.load(project));
  }
}

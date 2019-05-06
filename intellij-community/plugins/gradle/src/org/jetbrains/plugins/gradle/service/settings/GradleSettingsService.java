// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.settings.TestRunner;

/**
 * {@link GradleSettingsService} provides effective settings for linked gradle projects.
 *
 * @see GradleProjectSettings
 *
 * @author Vladislav.Soroka
 */
public class GradleSettingsService {
  @NotNull
  private final Project myProject;

  public GradleSettingsService(@NotNull Project project) {
    myProject = project;
  }

  public boolean isDelegatedBuildEnabled(@Nullable String gradleProjectPath) {
    GradleProjectSettings projectSettings = gradleProjectPath == null
                                            ? null : GradleSettings.getInstance(myProject).getLinkedProjectSettings(gradleProjectPath);
    if (projectSettings == null) return false;

    return projectSettings.getDelegatedBuild();
  }

  @NotNull
  public TestRunner getTestRunner(@Nullable String gradleProjectPath) {
    GradleProjectSettings projectSettings = gradleProjectPath == null
                                            ? null :GradleSettings.getInstance(myProject).getLinkedProjectSettings(gradleProjectPath);
    if (projectSettings == null) return TestRunner.PLATFORM;

    return projectSettings.getTestRunner();
  }

  public static boolean isDelegatedBuildEnabled(@NotNull Module module) {
    return getInstance(module.getProject()).isDelegatedBuildEnabled(ExternalSystemApiUtil.getExternalRootProjectPath(module));
  }

  @NotNull
  public static TestRunner getTestRunner(@NotNull Module module) {
    return getInstance(module.getProject()).getTestRunner(ExternalSystemApiUtil.getExternalRootProjectPath(module));
  }

  @NotNull
  public static GradleSettingsService getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, GradleSettingsService.class);
  }
}

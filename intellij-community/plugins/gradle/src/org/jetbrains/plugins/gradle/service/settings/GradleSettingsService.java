// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.DefaultGradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.settings.TestRunner;

/**
 * {@link GradleSettingsService} provides effective settings for linked gradle projects.
 *
 * @see GradleProjectSettings
 * @see DefaultGradleProjectSettings
 *
 * @author Vladislav.Soroka
 */
public class GradleSettingsService {
  @NotNull
  private final Project myProject;

  public GradleSettingsService(@NotNull Project project) {
    myProject = project;
  }

  public boolean isDelegatedBuildEnabled(@NotNull String gradleProjectPath) {
    GradleProjectSettings projectSettings = GradleSettings.getInstance(myProject).getLinkedProjectSettings(gradleProjectPath);
    if (projectSettings == null) return false;
    if (projectSettings.getDelegatedBuild() == ThreeState.UNSURE) {
      return DefaultGradleProjectSettings.getInstance(myProject).isDelegatedBuild();
    }
    return projectSettings.getDelegatedBuild().toBoolean();
  }

  @NotNull
  public TestRunner getTestRunner(@NotNull String gradleProjectPath) {
    GradleProjectSettings projectSettings = GradleSettings.getInstance(myProject).getLinkedProjectSettings(gradleProjectPath);
    return projectSettings == null || projectSettings.getTestRunner() == null
           ? DefaultGradleProjectSettings.getInstance(myProject).getTestRunner()
           : projectSettings.getTestRunner();
  }

  public static boolean isDelegatedBuildEnabled(@NotNull Module module) {
    String projectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module);
    if (projectPath == null) return false;
    return getInstance(module.getProject()).isDelegatedBuildEnabled(projectPath);
  }

  @NotNull
  public static TestRunner getTestRunner(@NotNull Module module) {
    String projectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module);
    if (projectPath == null) {
      return DefaultGradleProjectSettings.getInstance(module.getProject()).getTestRunner();
    }
    return getInstance(module.getProject()).getTestRunner(projectPath);
  }

  @NotNull
  public static GradleSettingsService getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, GradleSettingsService.class);
  }
}

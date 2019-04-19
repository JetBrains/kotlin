// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.settings.GradleSettingsService;

/**
 * {@link DefaultGradleProjectSettings} holds IDE project level settings defaults for gradle projects.
 *
 * @see GradleSettingsService
 *
 * @author Vladislav.Soroka
 */
@State(name = "DefaultGradleProjectSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class DefaultGradleProjectSettings implements PersistentStateComponent<DefaultGradleProjectSettings.MyState> {
  private boolean myDelegatedBuild = true;
  @NotNull private TestRunner myTestRunner = TestRunner.GRADLE;

  @NotNull
  public TestRunner getTestRunner() {
    return myTestRunner;
  }

  void setTestRunner(@NotNull TestRunner testRunner) {
    myTestRunner = testRunner;
  }

  public boolean isDelegatedBuild() {
    return myDelegatedBuild;
  }

  public void setDelegatedBuild(boolean delegatedBuild) {
    myDelegatedBuild = delegatedBuild;
  }

  @Nullable
  @Override
  public DefaultGradleProjectSettings.MyState getState() {
    MyState state = new MyState();
    state.delegatedBuild = myDelegatedBuild;
    state.testRunner = myTestRunner;
    return state;
  }

  @Override
  public void loadState(@NotNull MyState state) {
    myDelegatedBuild = state.delegatedBuild;
    myTestRunner = state.testRunner;
  }

  @NotNull
  public static DefaultGradleProjectSettings getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, DefaultGradleProjectSettings.class);
  }

  /**
   * Do not use the class directly. Consider to use {@link GradleSettingsService} or {@link GradleProjectSettings}
   */
  @ApiStatus.Experimental
  public static class MyState {
    public TestRunner testRunner = TestRunner.PLATFORM;
    public boolean delegatedBuild;
  }
}
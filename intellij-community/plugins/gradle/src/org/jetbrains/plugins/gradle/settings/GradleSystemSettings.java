// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
@State(name = "GradleSystemSettings", storages = @Storage("gradle.settings.xml"))
public class GradleSystemSettings implements PersistentStateComponent<GradleSystemSettings.MyState> {

  @Nullable private String myServiceDirectoryPath;
  @Nullable private String myGradleVmOptions;

  @NotNull
  public static GradleSystemSettings getInstance() {
    return ServiceManager.getService(GradleSystemSettings.class);
  }

  @Nullable
  @Override
  public GradleSystemSettings.MyState getState() {
    MyState state = new MyState();
    state.serviceDirectoryPath = myServiceDirectoryPath;
    state.gradleVmOptions = myGradleVmOptions;
    return state;
  }

  @Override
  public void loadState(@NotNull MyState state) {
    myServiceDirectoryPath = state.serviceDirectoryPath;
    myGradleVmOptions = state.gradleVmOptions;
  }

  @Nullable
  public String getServiceDirectoryPath() {
    return myServiceDirectoryPath;
  }

  public void setServiceDirectoryPath(@Nullable String newPath) {
    myServiceDirectoryPath = newPath;
  }

  @Nullable
  public String getGradleVmOptions() {
    return myGradleVmOptions;
  }

  public void setGradleVmOptions(@Nullable String gradleVmOptions) {
    myGradleVmOptions = gradleVmOptions;
  }

  /**
   * @see GradleSettings#isOfflineWork
   * @deprecated this settings parameter must be a project level
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
  public boolean isOfflineWork() {
    return false;
  }

  /**
   * @see GradleSettings#setOfflineWork
   * @deprecated this settings parameter must be a project level
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
  public void setOfflineWork(boolean isOfflineWork) {
  }

  public static class MyState {
    public String serviceDirectoryPath;
    public String gradleVmOptions;

    /**
     * @deprecated this settings parameter must be a project level
     */
    @Transient
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
    public boolean offlineWork;
  }
}
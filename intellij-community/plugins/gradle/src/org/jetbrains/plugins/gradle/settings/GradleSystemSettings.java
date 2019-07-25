// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
@State(name = "GradleSystemSettings", storages = @Storage("gradle.settings.xml"))
public class GradleSystemSettings implements PersistentStateComponent<GradleSystemSettings.MyState> {

  @Nullable private String myServiceDirectoryPath;
  @Nullable private String myGradleVmOptions;
  private boolean myIsOfflineWork;

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
    state.offlineWork = myIsOfflineWork;
    return state;
  }

  @Override
  public void loadState(@NotNull MyState state) {
    myServiceDirectoryPath = state.serviceDirectoryPath;
    myGradleVmOptions = state.gradleVmOptions;
    myIsOfflineWork = state.offlineWork;
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

  public boolean isOfflineWork() {
    return myIsOfflineWork;
  }

  public void setOfflineWork(boolean isOfflineWork) {
    myIsOfflineWork = isOfflineWork;
  }

  public static class MyState {
    public String serviceDirectoryPath;
    public String gradleVmOptions;
    public boolean offlineWork;
  }
}
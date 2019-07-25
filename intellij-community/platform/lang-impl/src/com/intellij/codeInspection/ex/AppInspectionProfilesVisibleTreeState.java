// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ex;

import com.intellij.codeInspection.InspectionProfile;
import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;

@State(
  name = "AppInspectionProfilesVisibleTreeState",
  storages = @Storage(value = "other.xml", roamingType = RoamingType.DISABLED)
)
public class AppInspectionProfilesVisibleTreeState implements PersistentStateComponent<VisibleTreeStateComponent> {
  private final VisibleTreeStateComponent myComponent = new VisibleTreeStateComponent();

  public static AppInspectionProfilesVisibleTreeState getInstance() {
    return ServiceManager.getService(AppInspectionProfilesVisibleTreeState.class);
  }

  @Override
  public VisibleTreeStateComponent getState() {
    return myComponent;
  }

  @Override
  public void loadState(@NotNull final VisibleTreeStateComponent state) {
    myComponent.copyFrom(state);
  }

  @NotNull
  public VisibleTreeState getVisibleTreeState(@NotNull InspectionProfile profile) {
    return myComponent.getVisibleTreeState(profile);
  }
}

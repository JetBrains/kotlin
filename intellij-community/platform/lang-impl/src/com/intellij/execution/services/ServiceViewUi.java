// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

interface ServiceViewUi {
  @NotNull
  JComponent getComponent();

  void saveState(@NotNull ServiceViewState state);

  void setServiceToolbar(@NotNull ServiceViewActionProvider actionManager);

  void setMasterComponent(@NotNull JComponent component, @NotNull ServiceViewActionProvider actionManager);

  void setDetailsComponent(@Nullable JComponent component);

  void setNavBar(@NotNull JComponent component);

  void setMasterComponentVisible(boolean visible);

  @Nullable
  JComponent getDetailsComponent();
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;

public class AddServiceActionGroup extends DefaultActionGroup {
  @Override
  public boolean canBePerformed(@NotNull DataContext context) {
    return false;
  }
}

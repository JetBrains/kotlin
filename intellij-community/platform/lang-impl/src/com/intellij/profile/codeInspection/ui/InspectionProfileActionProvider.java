// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.profile.codeInspection.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Bas Leijdekkers
 */
public abstract class InspectionProfileActionProvider {
  public static final ExtensionPointName<InspectionProfileActionProvider> EP_NAME =
    ExtensionPointName.create("com.intellij.inspectionProfileActionProvider");

  @NotNull
  public abstract List<AnAction> getActions(SingleInspectionProfilePanel panel);
}

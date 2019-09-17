// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.actions;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class GroupByConfigurationStatusAction extends RunDashboardGroupingRuleToggleAction {
  @NonNls private static final String NAME = "StatusDashboardGroupingRule";

  @NotNull
  @Override
  protected String getRuleName() {
    return NAME;
  }
}

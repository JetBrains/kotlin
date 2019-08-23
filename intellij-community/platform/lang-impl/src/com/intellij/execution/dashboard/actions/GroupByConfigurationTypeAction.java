// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.dashboard.tree.ConfigurationTypeDashboardGroupingRule;
import org.jetbrains.annotations.NotNull;

public class GroupByConfigurationTypeAction extends RunDashboardGroupingRuleToggleAction {
  @NotNull
  @Override
  protected String getRuleName() {
    return ConfigurationTypeDashboardGroupingRule.NAME;
  }
}

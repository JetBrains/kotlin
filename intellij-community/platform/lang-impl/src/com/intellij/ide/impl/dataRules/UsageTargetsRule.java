// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.impl.dataRules;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.usages.UsageTargetUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UsageTargetsRule implements GetDataRule {
  @Override
  @Nullable
  public Object getData(@NotNull DataProvider dataProvider) {
    return UsageTargetUtil.findUsageTargets(dataProvider);
  }
}

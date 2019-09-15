// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.configuration;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.remote.RemoteTargetConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RemoteTargetAwareRunProfile extends RunProfile {
  boolean canRunOn(@NotNull RemoteTargetConfiguration target);

  @Nullable
  String getDefaultTargetName();

  void setDefaultTargetName(@Nullable String targetName);
}

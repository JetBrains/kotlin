// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.progress;

import com.intellij.build.BuildDescriptor;
import com.intellij.build.events.BuildEventsNls;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Experimental
public interface BuildProgressDescriptor {
  @NotNull
  @BuildEventsNls.Title
  String getTitle();

  @NotNull
  BuildDescriptor getBuildDescriptor();
}

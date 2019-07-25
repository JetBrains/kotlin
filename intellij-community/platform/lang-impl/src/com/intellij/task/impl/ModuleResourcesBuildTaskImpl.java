// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.task.impl;

import com.intellij.openapi.module.Module;
import com.intellij.task.ModuleResourcesBuildTask;
import org.jetbrains.annotations.NotNull;

public class ModuleResourcesBuildTaskImpl extends ModuleBuildTaskImpl implements ModuleResourcesBuildTask {
  public ModuleResourcesBuildTaskImpl(@NotNull Module module) {
    super(module, true);
  }

  public ModuleResourcesBuildTaskImpl(@NotNull Module module, boolean isIncrementalBuild) {
    super(module, isIncrementalBuild);
  }
}

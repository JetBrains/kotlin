// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model;

import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

class BranchServiceImpl extends BranchService {
  @Override
  @NotNull ModelPatch performInBranch(@NotNull Project project, @NotNull Consumer<ModelBranch> action) {
    return PostprocessReformattingAspect.getInstance(project).postponeFormattingInside(
      () -> ModelBranchImpl.performInBranch(project, action));
  }
}
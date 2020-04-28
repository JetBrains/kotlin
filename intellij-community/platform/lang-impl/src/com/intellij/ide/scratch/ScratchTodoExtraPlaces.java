// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scratch;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.cache.impl.todo.TodoIndexers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScratchTodoExtraPlaces implements TodoIndexers.ExtraPlaceChecker {
  @Override
  public boolean accept(@Nullable Project project, @NotNull VirtualFile file) {
    return ScratchUtil.isScratch(file);
  }
}

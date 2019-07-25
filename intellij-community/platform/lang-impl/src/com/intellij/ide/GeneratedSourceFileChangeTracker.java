// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public abstract class GeneratedSourceFileChangeTracker {
  @NotNull
  public static GeneratedSourceFileChangeTracker getInstance(@NotNull Project project) {
    return project.getComponent(GeneratedSourceFileChangeTracker.class);
  }

  public abstract boolean isEditedGeneratedFile(@NotNull VirtualFile file);
}

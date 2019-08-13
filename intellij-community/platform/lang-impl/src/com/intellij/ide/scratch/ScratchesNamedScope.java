// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scratch;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.scope.packageSet.AbstractPackageSet;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScratchesNamedScope extends NamedScope {
  public static final String NAME = "Scratches and Consoles";

  public ScratchesNamedScope() {
    super(NAME, new AbstractPackageSet(NAME) {
      @Override
      public boolean contains(@NotNull VirtualFile file, NamedScopesHolder holder) {
        return contains(file, holder.getProject(), holder);
      }

      @Override
      public boolean contains(@NotNull VirtualFile file, @NotNull Project project, @Nullable NamedScopesHolder holder) {
        return ScratchUtil.isScratch(file);
      }
    });
  }
}

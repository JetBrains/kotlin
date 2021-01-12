// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scratch;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.scope.packageSet.AbstractPackageSet;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScratchesNamedScope extends NamedScope {
  public static String scratchesAndConsoles() { return IdeBundle.message("scratches.and.consoles"); }

  public ScratchesNamedScope() {
    super(scratchesAndConsoles(), AllIcons.Scope.Scratches, new AbstractPackageSet(scratchesAndConsoles()) {
      @Override
      public boolean contains(@NotNull VirtualFile file, NamedScopesHolder holder) {
        return ScratchesNamedScope.contains(holder.getProject(), file);
      }

      @Override
      public boolean contains(@NotNull VirtualFile file, @NotNull Project project, @Nullable NamedScopesHolder holder) {
        return ScratchesNamedScope.contains(project, file);
      }
    });
  }

  public static boolean contains(@NotNull Project project, @NotNull VirtualFile file) {
    RootType rootType = RootType.forFile(file);
    return rootType != null && !(rootType.isHidden() || rootType.isIgnored(project, file));
  }
}

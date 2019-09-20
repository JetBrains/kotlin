// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search.scope;

import com.intellij.ide.scratch.ScratchUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.scope.packageSet.FilteredPackageSet;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.ui.IdeUICustomization;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 * @author Sergey Malenkov
 */
public final class NonProjectFilesScope extends NamedScope {
  public static final String NAME = IdeUICustomization.getInstance().getNonProjectFilesScopeTitle();
  public static final NonProjectFilesScope INSTANCE = new NonProjectFilesScope();

  private NonProjectFilesScope() {
    super(NAME, new FilteredPackageSet(NAME) {
      @Override
      public boolean contains(@NotNull VirtualFile file, @NotNull Project project) {
        return containsImpl(file, project);
      }
    });
  }

  private static boolean containsImpl(@NotNull VirtualFile file,
                                      @NotNull Project project) {
    // do not include fake-files e.g. fragment-editors, etc.
    if (file.getFileSystem() instanceof NonPhysicalFileSystem) return false;
    if (!file.isInLocalFileSystem()) return true;
    if (ScratchUtil.isScratch(file)) return false;
    return !ProjectScope.getProjectScope(project).contains(file);
  }

  @Override
  public String getDefaultColorName() {
    return "Yellow";
  }
}

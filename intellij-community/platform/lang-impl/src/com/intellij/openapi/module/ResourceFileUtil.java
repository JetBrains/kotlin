// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.module;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.FilteredQuery;
import org.jetbrains.annotations.Nullable;

public final class ResourceFileUtil {
  private ResourceFileUtil() {
  }

  @Nullable
  public static VirtualFile findResourceFileInDependents(final Module searchFromModule, final String fileName) {
    return findResourceFileInScope(fileName, searchFromModule.getProject(), searchFromModule.getModuleWithDependenciesScope());
  }

  @Nullable
  public static VirtualFile findResourceFileInProject(final Project project, final String resourceName) {
    return findResourceFileInScope(resourceName, project, GlobalSearchScope.projectScope(project));
  }

  @Nullable
  public static VirtualFile findResourceFileInScope(final String resourceName,
                                                    final Project project,
                                                    final GlobalSearchScope scope) {
    int index = resourceName.lastIndexOf('/');
    String packageName = index >= 0 ? resourceName.substring(0, index).replace('/', '.') : "";
    final String fileName = index >= 0 ? resourceName.substring(index+1) : resourceName;

    final VirtualFile dir = new FilteredQuery<>(
      DirectoryIndex.getInstance(project).getDirectoriesByPackageName(packageName, false), file -> {
      final VirtualFile child = file.findChild(fileName);
      return child != null && scope.contains(child);
    }).findFirst();
    return dir != null ? dir.findChild(fileName) : null;
  }
}

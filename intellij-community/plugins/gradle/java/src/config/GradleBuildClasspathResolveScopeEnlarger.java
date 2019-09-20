// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.ResolveScopeEnlarger;
import com.intellij.psi.search.NonClasspathDirectoriesScope;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.GroovyFileType;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
final class GradleBuildClasspathResolveScopeEnlarger extends ResolveScopeEnlarger {
  @Override
  public SearchScope getAdditionalResolveScope(@NotNull VirtualFile file, Project project) {
    if (GroovyFileType.DEFAULT_EXTENSION.equals(file.getExtension())) {
      GradleClassFinder gradleClassFinder = PsiElementFinder.EP.findExtensionOrFail(GradleClassFinder.class, project);
      final List<VirtualFile> roots = gradleClassFinder.getClassRoots();
      for (VirtualFile root : roots) {
        if (VfsUtilCore.isAncestor(root, file, true)) {
          return NonClasspathDirectoriesScope.compose(roots);
        }
      }
    }
    return null;
  }
}

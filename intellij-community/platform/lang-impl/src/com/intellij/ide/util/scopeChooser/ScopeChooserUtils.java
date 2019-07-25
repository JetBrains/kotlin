// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.scopeChooser;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.scratch.ScratchesSearchScope;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.ChangeListsScopesProvider;
import com.intellij.psi.search.*;
import com.intellij.psi.search.scope.ProjectFilesScope;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ScopeChooserUtils {

  private static final String CURRENT_FILE_SCOPE_NAME = IdeBundle.message("scope.current.file");
  private static final String OPEN_FILES_SCOPE_NAME = IdeBundle.message("scope.open.files");

  private ScopeChooserUtils() {
  }

  /**
   * @return custom or standard scope with the provided name, i.e. scope that matches corresponding item from {@link ScopeChooserCombo}
   * with the following limitations:
   * <ul>
   * <li>module-specific scope is not handled: if <code>scopeName</code> is "Module 'foo'" then {@link ProjectFilesScope} is returned</li>
   * <li>each returned scope is intersected with the project content (the only exception is 'Scratches and Consoles' scope)</li>
   * <li>if no known scope with the provided name found then empty scope is returned</li>
   * </ul>
   */
  @NotNull
  public static GlobalSearchScope findScopeByName(@NotNull Project project, @Nullable String scopeName) {
    // logic here is similar to ScopeChooserCombo

    if (scopeName == null) return GlobalSearchScope.EMPTY_SCOPE;

    if (OPEN_FILES_SCOPE_NAME.equals(scopeName)) {
      return intersectWithContentScope(project, GlobalSearchScopes.openFilesScope(project));
    }

    if (CURRENT_FILE_SCOPE_NAME.equals(scopeName)) {
      VirtualFile[] array = FileEditorManager.getInstance(project).getSelectedFiles();
      List<VirtualFile> files = ContainerUtil.createMaybeSingletonList(ArrayUtil.getFirstElement(array));
      GlobalSearchScope scope = GlobalSearchScope.filesScope(project, files, CURRENT_FILE_SCOPE_NAME);
      return intersectWithContentScope(project, scope);
    }

    for (SearchScope scope: PredefinedSearchScopeProvider.getInstance()
                                                         .getPredefinedScopes(project, null, false, false, false, false, true)) {
      if (scope instanceof GlobalSearchScope && scope.getDisplayName().equals(scopeName)) {
        if (scope instanceof ScratchesSearchScope) {
          return (ScratchesSearchScope)scope;
        }
        return intersectWithContentScope(project, (GlobalSearchScope)scope);
      }
    }

    for (NamedScope scope: ChangeListsScopesProvider.getInstance(project).getFilteredScopes()) {
      if (scope.getName().equals(scopeName)) {
        return intersectWithContentScope(project, GlobalSearchScopesCore.filterScope(project, scope));
      }
    }

    for (NamedScopesHolder holder: NamedScopesHolder.getAllNamedScopeHolders(project)) {
      final NamedScope[] scopes = holder.getEditableScopes();  // predefined scopes already included
      for (NamedScope scope: scopes) {
        if (scope.getName().equals(scopeName)) {
          return intersectWithContentScope(project, GlobalSearchScopesCore.filterScope(project, scope));
        }
      }
    }

    if (scopeName.startsWith("Module '") && scopeName.endsWith("'")) {
      // Backward compatibility with previous File Watchers behavior.
      // It never worked correctly for scopes like "Module 'foo'" and always returned ProjectFilesScope in such cases.
      return ProjectScope.getContentScope(project);
    }

    return GlobalSearchScope.EMPTY_SCOPE;
  }

  @NotNull
  private static GlobalSearchScope intersectWithContentScope(@NotNull Project project, @NotNull GlobalSearchScope scope) {
    return scope.intersectWith(ProjectScope.getContentScope(project));
  }
}

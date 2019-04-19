// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scratch;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyKey;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectAndLibrariesScope;
import org.jetbrains.annotations.NotNull;

/**
 * @author gregsh
 */
public class ScratchesSearchScope extends GlobalSearchScope {

  private static final NotNullLazyKey<GlobalSearchScope, Project> SCRATCHES_SCOPE_KEY = NotNullLazyKey.create(
    "SCRATCHES_SCOPE_KEY",
    project -> new ScratchesSearchScope(project, ScratchFileService.getInstance()));
  
  @NotNull
  public static GlobalSearchScope getScratchesScope(@NotNull Project project) {
    return SCRATCHES_SCOPE_KEY.getValue(project);
  }

  private final ScratchFileService myService;

  private ScratchesSearchScope(@NotNull Project project, @NotNull ScratchFileService service) {
    super(project);
    myService = service;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return ScratchesNamedScope.NAME;
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    RootType rootType = myService.getRootType(file);
    return rootType != null && !rootType.isHidden();
  }

  @Override
  public boolean isSearchOutsideRootModel() {
    return true;
  }

  @Override
  public boolean isSearchInModuleContent(@NotNull Module aModule) {
    return false;
  }

  @Override
  public boolean isSearchInLibraries() {
    return false;
  }

  @NotNull
  @Override
  public GlobalSearchScope intersectWith(@NotNull GlobalSearchScope scope) {
    if (scope instanceof ProjectAndLibrariesScope) return this;
    return super.intersectWith(scope);
  }

  @Override
  public String toString() {
    return getDisplayName();
  }
}

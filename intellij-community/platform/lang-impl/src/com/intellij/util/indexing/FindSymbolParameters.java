// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.HiddenFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FindSymbolParameters {
  private final String myCompletePattern;
  private final String myLocalPatternName;
  private final GlobalSearchScope mySearchScope;
  private final IdFilter myIdFilter;

  public FindSymbolParameters(@NotNull String pattern,
                              @NotNull String name,
                              @NotNull GlobalSearchScope scope,
                              @Nullable IdFilter idFilter) {
    myCompletePattern = pattern;
    myLocalPatternName = name;
    mySearchScope = scope;
    myIdFilter = idFilter;
  }

  public FindSymbolParameters withCompletePattern(@NotNull String pattern) {
    return new FindSymbolParameters(pattern, myLocalPatternName, mySearchScope, myIdFilter);
  }

  public FindSymbolParameters withLocalPattern(@NotNull String pattern) {
    return new FindSymbolParameters(myCompletePattern, pattern, mySearchScope, myIdFilter);
  }

  public FindSymbolParameters withScope(@NotNull GlobalSearchScope scope) {
    return new FindSymbolParameters(myCompletePattern, myLocalPatternName, scope, myIdFilter);
  }

  @NotNull
  public String getCompletePattern() {
    return myCompletePattern;
  }

  @NotNull
  public String getLocalPatternName() {
    return myLocalPatternName;
  }

  @NotNull
  public GlobalSearchScope getSearchScope() {
    return mySearchScope;
  }

  @Nullable
  public IdFilter getIdFilter() {
    return myIdFilter;
  }

  @NotNull
  public Project getProject() {
    return ObjectUtils.notNull(mySearchScope.getProject());
  }

  public boolean isSearchInLibraries() {
    return mySearchScope.isSearchInLibraries();
  }

  public static FindSymbolParameters wrap(@NotNull String pattern, @NotNull Project project, boolean searchInLibraries) {
    return new FindSymbolParameters(pattern, pattern, searchScopeFor(project, searchInLibraries),
                                    ((FileBasedIndexImpl) FileBasedIndex.getInstance()).projectIndexableFiles(project));
  }

  public static FindSymbolParameters wrap(@NotNull String pattern, @NotNull GlobalSearchScope scope) {
    return new FindSymbolParameters(pattern, pattern, scope, null);
  }

  public static FindSymbolParameters simple(@NotNull Project project, boolean searchInLibraries) {
    return new FindSymbolParameters("", "", searchScopeFor(project, searchInLibraries),
                                    ((FileBasedIndexImpl) FileBasedIndex.getInstance()).projectIndexableFiles(project));
  }

  @NotNull
  public static GlobalSearchScope searchScopeFor(@Nullable Project project, boolean searchInLibraries) {
    GlobalSearchScope baseScope =
      project == null ? new EverythingGlobalScope() :
      searchInLibraries ? ProjectScope.getAllScope(project) : ProjectScope.getProjectScope(project);

    return baseScope.intersectWith(new EverythingGlobalScope(project) {
      @Override
      public boolean contains(@NotNull VirtualFile file) {
        return !(file.getFileSystem() instanceof HiddenFileSystem);
      }
    });
  }
}

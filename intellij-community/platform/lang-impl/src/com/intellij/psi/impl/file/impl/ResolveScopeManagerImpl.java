// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.file.impl;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.LibraryScopeCache;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.ResolveScopeManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchScopeUtil;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.AdditionalIndexableFileSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public final class ResolveScopeManagerImpl extends ResolveScopeManager {
  private final Project myProject;
  private final ProjectRootManager myProjectRootManager;
  private final PsiManager myManager;

  private final Map<VirtualFile, GlobalSearchScope> myDefaultResolveScopesCache;
  private final AdditionalIndexableFileSet myAdditionalIndexableFileSet;

  public ResolveScopeManagerImpl(Project project) {
    myProject = project;
    myProjectRootManager = ProjectRootManager.getInstance(project);
    myManager = PsiManager.getInstance(project);
    myAdditionalIndexableFileSet = new AdditionalIndexableFileSet(project);

    myDefaultResolveScopesCache = ConcurrentFactoryMap.create(
      key -> {
        GlobalSearchScope scope = null;
        for (ResolveScopeProvider resolveScopeProvider : ResolveScopeProvider.EP_NAME.getExtensionList()) {
          scope = resolveScopeProvider.getResolveScope(key, myProject);
          if (scope != null) break;
        }
        if (scope == null) scope = getInherentResolveScope(key);
        for (ResolveScopeEnlarger enlarger : ResolveScopeEnlarger.EP_NAME.getExtensions()) {
          SearchScope extra = enlarger.getAdditionalResolveScope(key, myProject);
          if (extra != null) {
            scope = scope.union(extra);
          }
        }
        return scope;
      },
      ContainerUtil::createConcurrentWeakKeySoftValueMap);

    ((PsiManagerImpl)myManager).registerRunnableToRunOnChange(myDefaultResolveScopesCache::clear);
    // Make it explicit that registering and removing ResolveScopeProviders needs to clear the resolve scope cache
    // (even though normally registerRunnableToRunOnChange would be enough to clear the cache)
    ResolveScopeProvider.EP_NAME.addExtensionPointListener(() -> myDefaultResolveScopesCache.clear(), project);
  }

  private GlobalSearchScope getResolveScopeFromProviders(@NotNull final VirtualFile vFile) {
    return myDefaultResolveScopesCache.get(vFile);
  }

  private GlobalSearchScope getInherentResolveScope(VirtualFile vFile) {
    ProjectFileIndex projectFileIndex = myProjectRootManager.getFileIndex();
    Module module = projectFileIndex.getModuleForFile(vFile);
    if (module != null) {
      boolean includeTests = TestSourcesFilter.isTestSources(vFile, myProject);
      return GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, includeTests);
    }

    if (!projectFileIndex.isInLibrary(vFile)) {
      GlobalSearchScope allScope = GlobalSearchScope.allScope(myProject);
      if (!allScope.contains(vFile)) {
        return GlobalSearchScope.fileScope(myProject, vFile).uniteWith(allScope);
      }
      return allScope;
    }

    return LibraryScopeCache.getInstance(myProject).getLibraryScope(projectFileIndex.getOrderEntriesForFile(vFile));
  }

  @Override
  @NotNull
  public GlobalSearchScope getResolveScope(@NotNull PsiElement element) {
    ProgressIndicatorProvider.checkCanceled();

    if (element instanceof PsiDirectory) {
      return getResolveScopeFromProviders(((PsiDirectory)element).getVirtualFile());
    }

    PsiFile containingFile = element.getContainingFile();
    if (containingFile instanceof PsiCodeFragment) {
      GlobalSearchScope forcedScope = ((PsiCodeFragment)containingFile).getForcedResolveScope();
      if (forcedScope != null) {
        return forcedScope;
      }
    }

    if (containingFile != null) {
      PsiElement context = containingFile.getContext();
      if (context != null) {
        return withFile(containingFile, getResolveScope(context));
      }
    }

    if (containingFile == null) {
      return GlobalSearchScope.allScope(myProject);
    }
    if (containingFile instanceof FileResolveScopeProvider) {
      return ((FileResolveScopeProvider)containingFile).getFileResolveScope();
    }
    VirtualFile vFile = containingFile.getOriginalFile().getVirtualFile();
    if (vFile == null) {
      return withFile(containingFile, GlobalSearchScope.allScope(myProject));
    }
    return getResolveScopeFromProviders(vFile);
  }

  private GlobalSearchScope withFile(PsiFile containingFile, GlobalSearchScope scope) {
    return PsiSearchScopeUtil.isInScope(scope, containingFile)
           ? scope
           : scope.uniteWith(GlobalSearchScope.fileScope(myProject, containingFile.getViewProvider().getVirtualFile()));
  }


  @NotNull
  @Override
  public GlobalSearchScope getDefaultResolveScope(@NotNull final VirtualFile vFile) {
    final PsiFile psiFile = myManager.findFile(vFile);
    assert psiFile != null : "directory=" + vFile.isDirectory() + "; " + myProject;
    return getResolveScopeFromProviders(vFile);
  }


  @Override
  @NotNull
  public GlobalSearchScope getUseScope(@NotNull PsiElement element) {
    VirtualFile vDirectory;
    final VirtualFile virtualFile;
    final PsiFile containingFile;
    final GlobalSearchScope allScope = GlobalSearchScope.allScope(myManager.getProject());
    if (element instanceof PsiDirectory) {
      vDirectory = ((PsiDirectory)element).getVirtualFile();
      virtualFile = null;
      containingFile = null;
    }
    else {
      containingFile = element.getContainingFile();
      if (containingFile == null) return allScope;
      virtualFile = containingFile.getVirtualFile();
      if (virtualFile == null) return allScope;
      if (virtualFile instanceof VirtualFileWindow) {
        return GlobalSearchScope.fileScope(myProject, ((VirtualFileWindow)virtualFile).getDelegate());
      }
      if ("Scratch".equals(virtualFile.getFileType().getName())) {
        return GlobalSearchScope.fileScope(myProject, virtualFile);
      }
      vDirectory = virtualFile.getParent();
    }

    if (vDirectory == null) return allScope;
    final ProjectFileIndex projectFileIndex = myProjectRootManager.getFileIndex();
    final Module module = projectFileIndex.getModuleForFile(vDirectory);
    if (module == null) {
      VirtualFile notNullVFile = virtualFile != null ? virtualFile : vDirectory;
      final List<OrderEntry> entries = projectFileIndex.getOrderEntriesForFile(notNullVFile);
      if (entries.isEmpty() && (myAdditionalIndexableFileSet.isInSet(notNullVFile) || isFromAdditionalLibraries(notNullVFile))) {
        return allScope;
      }

      GlobalSearchScope result = LibraryScopeCache.getInstance(myProject).getLibraryUseScope(entries);
      return containingFile == null || virtualFile.isDirectory() || result.contains(virtualFile)
             ? result : GlobalSearchScope.fileScope(containingFile).uniteWith(result);
    }
    boolean isTest = TestSourcesFilter.isTestSources(vDirectory, myProject);
    GlobalSearchScope scope = isTest
                              ? GlobalSearchScope.moduleTestsWithDependentsScope(module)
                              : GlobalSearchScope.moduleWithDependentsScope(module);
    RefResolveService resolveService;
    if (virtualFile instanceof VirtualFileWithId && RefResolveService.ENABLED && (resolveService = RefResolveService.getInstance(myProject)).isUpToDate()) {
      return resolveService.restrictByBackwardIds(virtualFile, scope);
    }
    return scope;
  }

  private boolean isFromAdditionalLibraries(@NotNull final VirtualFile file) {
    for (final AdditionalLibraryRootsProvider provider : AdditionalLibraryRootsProvider.EP_NAME.getExtensionList()) {
      for (final SyntheticLibrary library : provider.getAdditionalProjectLibraries(myProject)) {
        if (library.contains(file)) {
          return true;
        }
      }
    }
    return false;
  }
}
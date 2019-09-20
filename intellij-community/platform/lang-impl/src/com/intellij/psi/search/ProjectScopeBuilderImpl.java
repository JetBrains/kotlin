// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.core.CoreProjectScopeBuilder;
import com.intellij.ide.scratch.RootType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.UnloadedModuleDescription;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.impl.DirectoryInfo;
import com.intellij.openapi.roots.impl.ProjectFileIndexImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiBundle;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author yole
 */
public class ProjectScopeBuilderImpl extends ProjectScopeBuilder {

  protected final Project myProject;

  public ProjectScopeBuilderImpl(@NotNull Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public GlobalSearchScope buildEverythingScope() {
    return new EverythingGlobalScope(myProject) {
      @Override
      public boolean contains(@NotNull VirtualFile file) {
        RootType rootType = RootType.forFile(file);
        if (rootType != null && (rootType.isHidden() || rootType.isIgnored(myProject, file))) return false;
        return true;
      }
    };
  }

  @NotNull
  @Override
  public GlobalSearchScope buildLibrariesScope() {
    ProjectAndLibrariesScope result = new ProjectAndLibrariesScope(myProject) {
      @Override
      public boolean contains(@NotNull VirtualFile file) {
        return myProjectFileIndex.isInLibrary(file);
      }

      @Override
      public boolean isSearchInModuleContent(@NotNull Module aModule) {
        return false;
      }

      @NotNull
      @Override
      public Collection<UnloadedModuleDescription> getUnloadedModulesBelongingToScope() {
        return Collections.emptySet();
      }
    };
    result.setDisplayName(PsiBundle.message("psi.search.scope.libraries"));
    return result;
  }

  @NotNull
  @Override
  public GlobalSearchScope buildAllScope() {
    ProjectRootManager projectRootManager = myProject.isDefault() ? null : ProjectRootManager.getInstance(myProject);
    if (projectRootManager == null) {
      return new EverythingGlobalScope(myProject);
    }

    return new ProjectAndLibrariesScope(myProject) {
      @Override
      public boolean contains(@NotNull VirtualFile file) {
        DirectoryInfo info = ((ProjectFileIndexImpl)myProjectFileIndex).getInfoForFileOrDirectory(file);
        return info.isInProject(file) &&
               (info.getModule() != null || info.hasLibraryClassRoot() || info.isInLibrarySource(file));
      }
    };
  }

  @NotNull
  @Override
  public GlobalSearchScope buildProjectScope() {
    final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(myProject);
    if (projectRootManager == null) {
      return new EverythingGlobalScope(myProject) {
        @Override
        public boolean isSearchInLibraries() {
          return false;
        }
      };
    }
    return new ProjectScopeImpl(myProject, FileIndexFacade.getInstance(myProject));
  }

  @NotNull
  @Override
  public GlobalSearchScope buildContentScope() {
    return new CoreProjectScopeBuilder.ContentSearchScope(myProject, FileIndexFacade.getInstance(myProject));
  }
}

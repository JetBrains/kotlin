// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.file;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IconLayerProvider;
import com.intellij.ide.IconProvider;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.roots.impl.ProjectFileIndexImpl;
import com.intellij.openapi.roots.ui.configuration.SourceRootPresentation;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SourceRootIconProvider {
  @NotNull
  public static Icon getDirectoryIcon(VirtualFile vFile, Project project) {
    SourceFolder sourceFolder = ProjectRootsUtil.getModuleSourceRoot(vFile, project);
    if (sourceFolder != null) {
      return SourceRootPresentation.getSourceRootIcon(sourceFolder);
    }
    else {
      Icon excludedIcon = getIconIfExcluded(project, vFile);
      return excludedIcon != null ? excludedIcon : PlatformIcons.FOLDER_ICON;
    }
  }
  
  @Nullable
  public static Icon getIconIfExcluded(@NotNull Project project, @NotNull VirtualFile vFile) {
    if (!Registry.is("ide.hide.excluded.files")) {
      boolean ignored = ProjectRootManager.getInstance(project).getFileIndex().isExcluded(vFile);
      if (ignored) {
        return AllIcons.Modules.ExcludeRoot;
      }
    }
    return null;
  }

  @Nullable
  private static Icon calcFileLayerIcon(VirtualFile vFile, Project project) {
    ProjectFileIndexImpl index = (ProjectFileIndexImpl)ProjectFileIndex.getInstance(project);
    if (vFile != null) {
      VirtualFile parent = vFile.getParent();
      
      if (index.isExcluded(vFile)) {
        //If the parent directory is also excluded it'll have a special icon (see DirectoryIconProvider), so it makes no sense to add
        // additional marks for all files under it.
        if (parent == null || !index.isExcluded(parent)) {
          return AllIcons.Nodes.ExcludedFromCompile;
        }
      }
      else {
        SourceFolder sourceFolder = index.getSourceFolder(vFile);
        if (sourceFolder != null && vFile.equals(sourceFolder.getFile())) {
          SourceFolder parentSourceFolder = parent == null ? null : index.getSourceFolder(parent);

          // do not mark files under folder of the same root type (e.g. test root file under test root dir)
          // but mark file if they are under different root type (e.g. test root file under source root dir)
          if (parentSourceFolder == null || !sourceFolder.equals(parentSourceFolder)) {
            return SourceRootPresentation.getSourceRootFileLayerIcon(sourceFolder);
          }
        }
      }
    }
    return null;
  }

  public static class DirectoryProvider extends IconProvider implements DumbAware {
    @Override
    public Icon getIcon(@NotNull final PsiElement element, final int flags) {
      if (element instanceof PsiDirectory) {
        final PsiDirectory psiDirectory = (PsiDirectory)element;
        return getDirectoryIcon(psiDirectory.getVirtualFile(), psiDirectory.getProject());
      }
      return null;
    }
  }
  
  public static class FileLayerProvider implements IconLayerProvider, DumbAware {
    @Nullable
    @Override
    public Icon getLayerIcon(@NotNull Iconable element, boolean isLocked) {
      if (element instanceof PsiFile) {
        Project project = ((PsiFile)element).getProject();
        VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
        return CachedValuesManager.getCachedValue((PsiElement)element,
                                                  () -> CachedValueProvider.Result.create(calcFileLayerIcon(virtualFile, project),
                                                                                          ProjectRootManager.getInstance(project)));
      }
      return null;
    }
  
    @NotNull
    @Override
    public String getLayerDescription() {
      return "Source root files";
    }
  }
}

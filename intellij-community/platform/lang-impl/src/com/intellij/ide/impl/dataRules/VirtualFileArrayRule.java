// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.impl.dataRules;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileChooser.FileSystemTree;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDirectoryContainer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageDataUtil;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.UsageView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public class VirtualFileArrayRule implements GetDataRule {

  @Nullable private static Set<VirtualFile> addFiles(@Nullable Set<VirtualFile> set, VirtualFile[] files) {
    for (VirtualFile file : files) {
      set = addFile(set, file);
    }
    return set;
  }
  @Nullable private static Set<VirtualFile> addFile(@Nullable Set<VirtualFile> set, @Nullable VirtualFile file) {
    if (file == null) return set;
    if (set == null) set = new LinkedHashSet<>();
    set.add(file);
    return set;
  }

  @Override
  public Object getData(@NotNull final DataProvider dataProvider) {
    // Try to detect multiselection.

    Set<VirtualFile> result = null;

    FileSystemTree fileSystemTree = FileSystemTree.DATA_KEY.getData(dataProvider);
    if (fileSystemTree != null) {
      result = addFiles(result, fileSystemTree.getSelectedFiles());
    }
    else {
      Project project = PlatformDataKeys.PROJECT_CONTEXT.getData(dataProvider);
      if (project != null && !project.isDisposed()) {
        result = addFiles(result, ProjectRootManager.getInstance(project).getContentRoots());
      }

      Module[] selectedModules = LangDataKeys.MODULE_CONTEXT_ARRAY.getData(dataProvider);
      if (selectedModules != null && selectedModules.length > 0) {
        for (Module selectedModule : selectedModules) {
          result = addFiles(result, ModuleRootManager.getInstance(selectedModule).getContentRoots());
        }
      }

      Module selectedModule = LangDataKeys.MODULE_CONTEXT.getData(dataProvider);
      if (selectedModule != null && !selectedModule.isDisposed()) {
        result = addFiles(result, ModuleRootManager.getInstance(selectedModule).getContentRoots());
      }
    }


    PsiElement[] psiElements = LangDataKeys.PSI_ELEMENT_ARRAY.getData(dataProvider);
    if (psiElements != null) {
      for (PsiElement element : psiElements) {
        result = addFilesFromPsiElement(result, element);
      }
    }

    result = addFile(result, CommonDataKeys.VIRTUAL_FILE.getData(dataProvider));

    PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(dataProvider);
    if (psiFile != null) {
      result = addFile(result, psiFile.getVirtualFile());
    }

    if (result != null) {
      return VfsUtilCore.toVirtualFileArray(result);
    }

    PsiElement elem = CommonDataKeys.PSI_ELEMENT.getData(dataProvider);
    if (elem != null) {
      result = addFilesFromPsiElement(result, elem);
    }

    Usage[] usages = UsageView.USAGES_KEY.getData(dataProvider);
    UsageTarget[] usageTargets = UsageView.USAGE_TARGETS_KEY.getData(dataProvider);
    if (usages != null || usageTargets != null) {
      for (VirtualFile file : UsageDataUtil.provideVirtualFileArray(usages, usageTargets)) {
        result = addFile(result, file);
      }
    }

    if (result == null) {
      final Object[] objects = (Object[])dataProvider.getData(PlatformDataKeys.SELECTED_ITEMS.getName());
      if (objects != null) {
        final VirtualFile[] files = new VirtualFile[objects.length];
        for (int i = 0, objectsLength = objects.length; i < objectsLength; i++) {
          Object object = objects[i];
          if (!(object instanceof VirtualFile)) return null;
          files[i] = (VirtualFile)object;
        }

        return files;
      }
      return null;
    }
    else {
      return VfsUtilCore.toVirtualFileArray(result);
    }
  }


  private static Set<VirtualFile> addFilesFromPsiElement(Set<VirtualFile> files, @NotNull PsiElement elem) {
    if (elem instanceof PsiDirectory) {
      files = addFile(files, ((PsiDirectory)elem).getVirtualFile());
    }
    else if (elem instanceof PsiFile) {
      files = addFile(files, ((PsiFile)elem).getVirtualFile());
    }
    else if (elem instanceof PsiDirectoryContainer) {
      for (PsiDirectory dir : ((PsiDirectoryContainer)elem).getDirectories()) {
        files = addFile(files, dir.getVirtualFile());
      }
    }
    else {
      PsiFile file = elem.getContainingFile();
      if (file != null) {
        files = addFile(files, file.getVirtualFile());
      }
    }
    return files;
  }

}

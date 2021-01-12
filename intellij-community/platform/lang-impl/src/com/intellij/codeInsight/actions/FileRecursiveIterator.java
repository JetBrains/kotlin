// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.actions;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.CompactVirtualFileSet;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class FileRecursiveIterator {
  @NotNull private final Project myProject;
  @NotNull private final Collection<? extends VirtualFile> myRoots;

  FileRecursiveIterator(@NotNull Project project, @NotNull List<? extends PsiFile> roots) {
    this(project, ContainerUtil.<PsiFile, VirtualFile>map(roots, psiDir -> psiDir.getVirtualFile()));
  }

  FileRecursiveIterator(@NotNull Module module) {
    this(module.getProject(), ContainerUtil.<PsiDirectory, VirtualFile>map(collectModuleDirectories(module), psiDir -> psiDir.getVirtualFile()));
  }

  FileRecursiveIterator(@NotNull Project project) {
    this(project, ContainerUtil.<PsiDirectory, VirtualFile>map(collectProjectDirectories(project), psiDir -> psiDir.getVirtualFile()));
  }

  FileRecursiveIterator(@NotNull PsiDirectory directory) {
    this(directory.getProject(), Collections.singletonList(directory.getVirtualFile()));
  }

  FileRecursiveIterator(@NotNull Project project, @NotNull Collection<? extends VirtualFile> roots) {
    myProject = project;
    myRoots = roots;
  }

  @NotNull
  static List<PsiDirectory> collectProjectDirectories(@NotNull Project project) {
    Module[] modules = ModuleManager.getInstance(project).getModules();
    List<PsiDirectory> directories = new ArrayList<>(modules.length*3);
    for (Module module : modules) {
      directories.addAll(collectModuleDirectories(module));
    }

    return directories;
  }

  boolean processAll(@NotNull Processor<? super PsiFile> processor) {
    CompactVirtualFileSet visited = new CompactVirtualFileSet();
    for (VirtualFile root : myRoots) {
      if (!ProjectRootManager.getInstance(myProject).getFileIndex().iterateContentUnderDirectory(root, fileOrDir -> {
        if (fileOrDir.isDirectory() || !visited.add(fileOrDir)) {
          return true;
        }
        PsiFile psiFile = ReadAction.compute(() -> myProject.isDisposed() ? null : PsiManager.getInstance(myProject).findFile(fileOrDir));
        return psiFile == null || processor.process(psiFile);
      })) return false;
    }
    return true;
  }

  @NotNull
  static List<PsiDirectory> collectModuleDirectories(Module module) {
    VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
    return ReadAction.compute(() -> ContainerUtil.mapNotNull(contentRoots, root -> PsiManager.getInstance(module.getProject()).findDirectory(root)));
  }
}

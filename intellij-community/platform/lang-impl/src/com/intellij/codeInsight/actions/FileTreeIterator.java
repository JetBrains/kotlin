// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.actions;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FileTreeIterator {
  private Queue<PsiFile> myCurrentFiles = new LinkedList<>();
  private Queue<PsiDirectory> myCurrentDirectories = new LinkedList<>();

  public FileTreeIterator(@NotNull List<? extends PsiFile> files) {
    myCurrentFiles.addAll(files);
  }

  public FileTreeIterator(@NotNull Module module) {
    myCurrentDirectories.addAll(collectModuleDirectories(module));
    expandDirectoriesUntilFilesNotEmpty();
  }

  public FileTreeIterator(@NotNull Project project) {
    myCurrentDirectories.addAll(collectProjectDirectories(project));
    expandDirectoriesUntilFilesNotEmpty();
  }

  @NotNull
  public static List<PsiDirectory> collectProjectDirectories(@NotNull Project project) {
    List<PsiDirectory> directories = new ArrayList<>();

    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      directories.addAll(collectModuleDirectories(module));
    }

    return directories;
  }

  public FileTreeIterator(@NotNull PsiDirectory directory) {
    myCurrentDirectories.add(directory);
    expandDirectoriesUntilFilesNotEmpty();
  }

  public FileTreeIterator(@NotNull FileTreeIterator fileTreeIterator) {
    myCurrentFiles = new LinkedList<>(fileTreeIterator.myCurrentFiles);
    myCurrentDirectories = new LinkedList<>(fileTreeIterator.myCurrentDirectories);
  }

  @NotNull
  public PsiFile next() {
    if (myCurrentFiles.isEmpty()) {
      throw new NoSuchElementException();
    }
    PsiFile current = myCurrentFiles.poll();
    expandDirectoriesUntilFilesNotEmpty();
    return current;
  }

  public boolean hasNext() {
    return !myCurrentFiles.isEmpty();
  }

  private void expandDirectoriesUntilFilesNotEmpty() {
    while (myCurrentFiles.isEmpty() && !myCurrentDirectories.isEmpty()) {
      PsiDirectory dir = myCurrentDirectories.poll();
      expandDirectory(dir);
    }
  }

  private void expandDirectory(@NotNull PsiDirectory dir) {
    Collections.addAll(myCurrentFiles, dir.getFiles());
    Collections.addAll(myCurrentDirectories, dir.getSubdirectories());
  }

  @NotNull
  public static List<PsiDirectory> collectModuleDirectories(Module module) {
    List<PsiDirectory> dirs = new ArrayList<>();

    VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
    for (VirtualFile root : contentRoots) {
      PsiDirectory dir = PsiManager.getInstance(module.getProject()).findDirectory(root);
      if (dir != null) {
        dirs.add(dir);
      }
    }

    return dirs;
  }
}

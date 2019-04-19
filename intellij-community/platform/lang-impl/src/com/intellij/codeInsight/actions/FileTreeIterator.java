/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInsight.actions;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.ContainerUtil;
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
    List<PsiDirectory> directories = ContainerUtil.newArrayList();

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
    List<PsiDirectory> dirs = ContainerUtil.newArrayList();

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

/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.compiler.backwardRefs.view;

import com.intellij.compiler.CompilerDirectHierarchyInfo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.stream.Stream;

public class CompilerReferenceHierarchyTestInfo {
  @Nullable private final CompilerDirectHierarchyInfo myHierarchyInfo;
  @NotNull private final DirtyScopeTestInfo myDirtyScopeInfo;

  public CompilerReferenceHierarchyTestInfo(@Nullable CompilerDirectHierarchyInfo hierarchyInfo,
                                            @NotNull DirtyScopeTestInfo dirtyScopeTestInfo) {
    myHierarchyInfo = hierarchyInfo;
    myDirtyScopeInfo = dirtyScopeTestInfo;
  }

  @NotNull
  public Stream<PsiElement> getHierarchyChildren() {
    if (myHierarchyInfo == null) throw new IllegalArgumentException();
    return myHierarchyInfo.getHierarchyChildren();
  }

  @NotNull
  Module[] getDirtyModules() {
    return myDirtyScopeInfo.getDirtyModules();
  }

  @NotNull
  Module[] getDirtyUnsavedModules() {
    return myDirtyScopeInfo.getDirtyUnsavedModules();
  }

  @NotNull
  VirtualFile[] getExcludedFiles() {
    return myDirtyScopeInfo.getExcludedFiles();
  }

  boolean isEnabled() {
    return myHierarchyInfo != null;
  }

  DefaultMutableTreeNode asTree() {
    DefaultMutableTreeNode node = new DefaultMutableTreeNode();
    if (isEnabled()) {
      final DefaultMutableTreeNode knownOccurrences = new DefaultMutableTreeNode("Known hierarchy direct children");
      node.add(knownOccurrences);
      getHierarchyChildren().forEach(e -> knownOccurrences.add(new DefaultMutableTreeNode(e)));

      final DefaultMutableTreeNode dirtyModules = new DefaultMutableTreeNode("Dirty modules");
      node.add(dirtyModules);
      for (Module module : getDirtyModules()) {
        dirtyModules.add(new DefaultMutableTreeNode(module));
      }

      final DefaultMutableTreeNode unsavedDirtyModules = new DefaultMutableTreeNode("Unsaved dirty modules");
      node.add(unsavedDirtyModules);
      for (Module module : getDirtyUnsavedModules()) {
        unsavedDirtyModules.add(new DefaultMutableTreeNode(module));
      }

      final DefaultMutableTreeNode excludedFiles = new DefaultMutableTreeNode("Current excluded files");
      node.add(excludedFiles);
      for (VirtualFile excludedFile : getExcludedFiles()) {
        excludedFiles.add(new DefaultMutableTreeNode(excludedFile));
      }
    }
    else {
      node.add(new DefaultMutableTreeNode("Service is not available"));
    }

    return node;
  }
}

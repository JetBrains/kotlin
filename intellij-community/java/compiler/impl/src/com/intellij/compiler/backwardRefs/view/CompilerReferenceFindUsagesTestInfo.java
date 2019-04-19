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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.FileBasedIndex;
import gnu.trove.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.stream.IntStream;

public class CompilerReferenceFindUsagesTestInfo {
  @Nullable private final TIntHashSet myFileIds;
  @NotNull private final DirtyScopeTestInfo myDirtyScopeInfo;
  @NotNull private final Project myProject;

  public CompilerReferenceFindUsagesTestInfo(@Nullable TIntHashSet occurrencesIds,
                                             @NotNull DirtyScopeTestInfo dirtyScopeTestInfo,
                                             @NotNull Project project) {
    myFileIds = occurrencesIds;
    myDirtyScopeInfo = dirtyScopeTestInfo;
    myProject = project;
  }

  @NotNull
  VirtualFile[] getFilesWithKnownOccurrences() {
    if (myFileIds == null) throw new IllegalStateException();
    final FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
    return IntStream.of(myFileIds.toArray())
      .mapToObj(id -> fileBasedIndex.findFileById(myProject, id))
      .filter(f -> !myDirtyScopeInfo.getDirtyScope().contains(f))
      .toArray(VirtualFile[]::new);
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
    return myFileIds != null;
  }

  DefaultMutableTreeNode asTree() {
    DefaultMutableTreeNode node = new DefaultMutableTreeNode();
    if (isEnabled()) {
      final DefaultMutableTreeNode knownOccurrences = new DefaultMutableTreeNode("Known occurrence files");
      node.add(knownOccurrences);
      for (VirtualFile file : getFilesWithKnownOccurrences()) {
        knownOccurrences.add(new DefaultMutableTreeNode(file));
      }

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

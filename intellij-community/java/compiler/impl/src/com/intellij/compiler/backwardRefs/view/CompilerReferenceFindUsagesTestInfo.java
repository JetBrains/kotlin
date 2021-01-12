// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.backwardRefs.view;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;

public final class CompilerReferenceFindUsagesTestInfo {
  @Nullable private final IntSet myFileIds;
  @NotNull private final DirtyScopeTestInfo myDirtyScopeInfo;

  public CompilerReferenceFindUsagesTestInfo(@Nullable IntSet occurrencesIds, @NotNull DirtyScopeTestInfo dirtyScopeTestInfo) {
    myFileIds = occurrencesIds;
    myDirtyScopeInfo = dirtyScopeTestInfo;
  }

  private @NotNull List<VirtualFile> getFilesWithKnownOccurrences() {
    if (myFileIds == null) {
      throw new IllegalStateException();
    }

    ManagingFS managingFs = ManagingFS.getInstance();
    List<VirtualFile> list = new ArrayList<>();
    for (IntIterator iterator = myFileIds.iterator(); iterator.hasNext(); ) {
      VirtualFile f = managingFs.findFileById(iterator.nextInt());
      if (f != null && !myDirtyScopeInfo.getDirtyScope().contains(f)) {
        list.add(f);
      }
    }
    return list;
  }

  private Module @NotNull [] getDirtyModules() {
    return myDirtyScopeInfo.getDirtyModules();
  }

  private Module @NotNull [] getDirtyUnsavedModules() {
    return myDirtyScopeInfo.getDirtyUnsavedModules();
  }

  private VirtualFile @NotNull [] getExcludedFiles() {
    return myDirtyScopeInfo.getExcludedFiles();
  }

  private boolean isEnabled() {
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

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.hash.SharedIndexChunkConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.*;

final class FileBasedIndexScanRunnableCollectorImpl extends FileBasedIndexScanRunnableCollector {
  private final Project myProject;
  private final ProjectFileIndex myProjectFileIndex;
  private final boolean myDisabled;

  FileBasedIndexScanRunnableCollectorImpl(@NotNull Project project) {
    myProject = project;
    myProjectFileIndex = ProjectFileIndex.getInstance(myProject);
    myDisabled = LightEdit.owns(myProject);
  }

  @Override
  public boolean shouldCollect(@NotNull VirtualFile file) {
    if (myDisabled) return false;
    if (myProjectFileIndex.isInContent(file) || myProjectFileIndex.isInLibrary(file)) {
      return !FileTypeManager.getInstance().isFileIgnored(file);
    }
    return false;
  }

  @Override
  public List<Runnable> collectScanRootRunnables(@NotNull ContentIterator processor, ProgressIndicator indicator) {
    if (myDisabled) {
      return Collections.emptyList();
    }
    return ReadAction.compute(() -> {
      if (myProject.isDisposed()) {
        return Collections.emptyList();
      }

      List<Runnable> tasks = new ArrayList<>();
      final Set<VirtualFile> visitedRoots = ContainerUtil.newConcurrentSet();

      tasks.add(() -> myProjectFileIndex.iterateContent(processor, file -> !file.isDirectory() || visitedRoots.add(file)));

      Set<VirtualFile> contributedRoots = new LinkedHashSet<>();
      for (IndexableSetContributor contributor : IndexableSetContributor.EP_NAME.getExtensionList()) {
        //important not to depend on project here, to support per-project background reindex
        // each client gives a project to FileBasedIndex
        if (myProject.isDisposed()) {
          return tasks;
        }
        contributedRoots.addAll(IndexableSetContributor.getRootsToIndex(contributor));
        contributedRoots.addAll(IndexableSetContributor.getProjectRootsToIndex(contributor, myProject));
      }
      for (VirtualFile root : contributedRoots) {
        // do not try to visit under-content-roots because the first task took care of that already
        if (!myProjectFileIndex.isInContent(root) && visitedRoots.add(root)) {
          tasks.add(() -> {
            if (myProject.isDisposed() || !root.isValid()) return;
            FileBasedIndex.iterateRecursively(root, processor, indicator, visitedRoots, null);
          });
        }
      }

      // iterate synthetic project libraries
      for (AdditionalLibraryRootsProvider provider : AdditionalLibraryRootsProvider.EP_NAME.getExtensionList()) {
        if (myProject.isDisposed()) {
          return tasks;
        }
        for (SyntheticLibrary library : provider.getAdditionalProjectLibraries(myProject)) {
          for (VirtualFile root : library.getAllRoots()) {
            // do not try to visit under-content-roots because the first task took care of that already
            if (!myProjectFileIndex.isInContent(root) && visitedRoots.add(root)) {
              tasks.add(() -> {
                if (myProject.isDisposed() || !root.isValid()) return;
                FileBasedIndex.iterateRecursively(root, processor, indicator, visitedRoots, myProjectFileIndex);
              });
            }
          }
        }
      }

      // iterate associated libraries
      Set<OrderEntry> allEntries = new HashSet<>();

      for (final Module module : ModuleManager.getInstance(myProject).getModules()) {
        OrderEntry[] orderEntries = ModuleRootManager.getInstance(module).getOrderEntries();
        for (OrderEntry orderEntry : orderEntries) {
          allEntries.add(orderEntry);
          if (!(orderEntry instanceof LibraryOrSdkOrderEntry) || !orderEntry.isValid()) {
            continue;
          }

          final LibraryOrSdkOrderEntry entry = (LibraryOrSdkOrderEntry)orderEntry;
          final VirtualFile[] libSources = entry.getRootFiles(OrderRootType.SOURCES);
          final VirtualFile[] libClasses = entry.getRootFiles(OrderRootType.CLASSES);
          for (VirtualFile[] roots : new VirtualFile[][]{libSources, libClasses}) {
            for (final VirtualFile root : roots) {
              // do not try to visit under-content-roots because the first task took care of that already
              if (!myProjectFileIndex.isInContent(root) && visitedRoots.add(root)) {
                tasks.add(() -> {
                  if (myProject.isDisposed() || module.isDisposed() || !root.isValid()) return;
                  FileBasedIndex.iterateRecursively(root, processor, indicator, visitedRoots, myProjectFileIndex);
                });
              }
            }
          }
        }
      }

      SharedIndexChunkConfiguration.getInstance().locateIndexes(myProject, allEntries, indicator);

      return tasks;
    });
  }
}
